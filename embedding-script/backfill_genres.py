"""Backfill catalog_embedding genre_ids and vote_count from TMDB detail."""

from __future__ import annotations

import argparse
import logging
import os
import sys
import threading
import time
from collections import Counter
from collections.abc import Callable, Iterable, Sequence
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

import psycopg2
from dotenv import load_dotenv

from genre_vocab import KNOWN_GENRE_IDS, to_canonical
from tmdb_source import TMDB_API_HOST, create_session

WORKER_COUNT = 8
REQUESTS_PER_SECOND = 20
REQUEST_TIMEOUT_S = 10.0
BATCH_SIZE = 200
MIN_REQUEST_INTERVAL_S = 1.0 / REQUESTS_PER_SECOND

SELECT_NULL_SQL = """
SELECT tmdb_id, media_type
FROM catalog_embedding
WHERE genre_ids IS NULL
ORDER BY tmdb_id, media_type
"""

COUNT_NULL_SQL = """
SELECT count(*)
FROM catalog_embedding
WHERE genre_ids IS NULL
"""

UPDATE_SQL = """
UPDATE catalog_embedding
SET genre_ids = %s,
    vote_count = %s
WHERE tmdb_id = %s
  AND media_type = %s
"""

logger = logging.getLogger(__name__)

RowKey = tuple[int, str]
UpdateRow = tuple[int, str, list[int], int]


class SharedRateLimiter:
    """Thread-safe minimum gap between successive waits."""

    def __init__(
        self,
        min_interval_s: float,
        clock: Callable[[], float],
        sleep: Callable[[float], None],
    ) -> None:
        self._min_interval_s = min_interval_s
        self._clock = clock
        self._sleep = sleep
        self._lock = threading.Lock()
        self._last_request_at: float | None = None

    def wait(self) -> None:
        """Block until the next request slot is free."""
        with self._lock:
            now = self._clock()
            if self._last_request_at is not None:
                remaining = self._min_interval_s - (now - self._last_request_at)
                if remaining > 0:
                    self._sleep(remaining)
                    now = self._clock()
            self._last_request_at = now


def require_env() -> dict[str, str]:
    """Load TMDB and rec-write DB settings from the environment."""
    load_dotenv()
    values: dict[str, str] = {}
    for name in ("TMDB_API_KEY", "REC_WRITE_DATABASE_URL"):
        raw = os.environ.get(name)
        if raw is None or not raw.strip():
            raise SystemExit(f"Missing required environment variable: {name}")
        values[name] = raw.strip()
    return values


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Parse CLI args. Optional --limit caps how many NULL rows to process."""
    parser = argparse.ArgumentParser(
        description="Backfill genre_ids and vote_count on catalog_embedding"
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Process at most N rows where genre_ids IS NULL",
    )
    args = parser.parse_args(argv)
    if args.limit is not None and args.limit < 1:
        parser.error("--limit must be >= 1")
    return args


def detail_path(media_type: str, tmdb_id: int) -> str:
    """Build the TMDB detail path for one catalog row."""
    if media_type == "MOVIE":
        return f"/3/movie/{tmdb_id}"
    if media_type == "TV":
        return f"/3/tv/{tmdb_id}"
    raise ValueError(f"unsupported media_type: {media_type}")


def detail_payload_to_row(
    media_type: str,
    payload: dict[str, Any],
) -> tuple[list[int], int]:
    """Map a TMDB detail payload to canonical genre_ids and vote_count."""
    genres_raw = payload.get("genres")
    if genres_raw is None:
        genre_ids: list[int] = []
    else:
        genre_ids = [
            int(item["id"])
            for item in genres_raw
            if isinstance(item, dict) and "id" in item
        ]
    canonical = to_canonical(media_type, genre_ids)
    vote_count = int(payload.get("vote_count") or 0)
    return canonical, vote_count


def iter_batches(rows: Sequence[Any], size: int) -> Iterable[Sequence[Any]]:
    """Split a sequence into contiguous batches of at most size items."""
    if size < 1:
        raise ValueError("batch size must be >= 1")
    for start in range(0, len(rows), size):
        yield rows[start : start + size]


def _retry_after_seconds(headers: Any) -> float:
    """Read Retry-After seconds, or 1 when the header is missing."""
    raw = None
    if headers is not None:
        raw = headers.get("Retry-After")
    if raw is not None and str(raw).strip().isdigit():
        return float(raw)
    return 1.0


def fetch_detail(
    session: Any,
    media_type: str,
    tmdb_id: int,
    limiter: SharedRateLimiter,
    *,
    sleep: Callable[[float], None] = time.sleep,
) -> tuple[str, dict[str, Any] | None]:
    """GET one detail payload. Returns (ok|not_found|failed, payload|None)."""
    url = f"{TMDB_API_HOST}{detail_path(media_type, tmdb_id)}"
    for attempt in range(2):
        limiter.wait()
        try:
            response = session.get(url, timeout=REQUEST_TIMEOUT_S)
        except Exception:
            return "failed", None
        status = int(response.status_code)
        if status == 200:
            try:
                return "ok", response.json()
            except Exception:
                return "failed", None
        if status == 404:
            return "not_found", None
        if status == 429 and attempt == 0:
            sleep(_retry_after_seconds(response.headers))
            continue
        return "failed", None
    return "failed", None


def select_null_genre_rows(conn, limit: int | None) -> list[RowKey]:
    """Load catalog rows that still need genre backfill."""
    sql = SELECT_NULL_SQL
    params: tuple[Any, ...] = ()
    if limit is not None:
        sql = SELECT_NULL_SQL + " LIMIT %s"
        params = (limit,)
    with conn.cursor() as cur:
        cur.execute(sql, params)
        return [(int(tmdb_id), str(media_type)) for tmdb_id, media_type in cur.fetchall()]


def count_null_genre_rows(conn) -> int:
    """Count rows that still have genre_ids NULL."""
    with conn.cursor() as cur:
        cur.execute(COUNT_NULL_SQL)
        row = cur.fetchone()
        return int(row[0]) if row is not None else 0


def write_update_batch(conn, rows: Sequence[UpdateRow]) -> None:
    """Write one batch of genre/vote updates and commit."""
    if not rows:
        return
    with conn.cursor() as cur:
        cur.executemany(
            UPDATE_SQL,
            [
                (genre_ids, vote_count, tmdb_id, media_type)
                for tmdb_id, media_type, genre_ids, vote_count in rows
            ],
        )
    conn.commit()


def _fetch_one(
    session: Any,
    limiter: SharedRateLimiter,
    tmdb_id: int,
    media_type: str,
) -> tuple[str, UpdateRow | None]:
    """Fetch and map one row. Never raises for a single bad row."""
    try:
        status, payload = fetch_detail(session, media_type, tmdb_id, limiter)
        if status != "ok" or payload is None:
            return status, None
        genre_ids, vote_count = detail_payload_to_row(media_type, payload)
        return "ok", (tmdb_id, media_type, genre_ids, vote_count)
    except Exception:
        logger.exception("row failed tmdb_id=%s media_type=%s", tmdb_id, media_type)
        return "failed", None


def summarize(
    selected: int,
    updated_rows: Sequence[UpdateRow],
    not_found: int,
    failed: int,
    still_null: int,
) -> str:
    """Build the one-line end summary for stdout."""
    empty_genres = sum(1 for _t, _m, genres, _v in updated_rows if len(genres) == 0)
    counts: Counter[int] = Counter()
    for _t, _m, genres, _v in updated_rows:
        counts.update(genres)
    top10 = counts.most_common(10)
    unknown = sorted(gid for gid in counts if gid not in KNOWN_GENRE_IDS)
    return (
        f"selected={selected} updated={len(updated_rows)} "
        f"not_found_404={not_found} other_failures={failed} "
        f"still_null={still_null} updated_empty_genres={empty_genres} "
        f"top10_canonical_ids={top10} unknown_canonical_ids={unknown}"
    )


def main(argv: list[str] | None = None) -> None:
    """Select NULL genre rows, fetch TMDB detail, write batches."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    args = parse_args(argv)
    env = require_env()

    conn = None
    try:
        conn = psycopg2.connect(env["REC_WRITE_DATABASE_URL"])
        selected_rows = select_null_genre_rows(conn, args.limit)
        session = create_session(env["TMDB_API_KEY"])
        limiter = SharedRateLimiter(
            MIN_REQUEST_INTERVAL_S,
            time.monotonic,
            time.sleep,
        )

        updated_rows: list[UpdateRow] = []
        pending: list[UpdateRow] = []
        not_found = 0
        failed = 0

        with ThreadPoolExecutor(max_workers=WORKER_COUNT) as pool:
            futures = {
                pool.submit(_fetch_one, session, limiter, tmdb_id, media_type): (
                    tmdb_id,
                    media_type,
                )
                for tmdb_id, media_type in selected_rows
            }
            for future in as_completed(futures):
                status, row = future.result()
                if status == "ok" and row is not None:
                    pending.append(row)
                    if len(pending) >= BATCH_SIZE:
                        write_update_batch(conn, pending)
                        updated_rows.extend(pending)
                        pending = []
                elif status == "not_found":
                    not_found += 1
                else:
                    failed += 1

        if pending:
            write_update_batch(conn, pending)
            updated_rows.extend(pending)

        still_null = count_null_genre_rows(conn)
        print(
            summarize(
                len(selected_rows),
                updated_rows,
                not_found,
                failed,
                still_null,
            )
        )
    finally:
        if conn is not None:
            conn.close()


if __name__ == "__main__":
    main()
    sys.exit(0)
