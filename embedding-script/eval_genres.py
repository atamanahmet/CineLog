"""Eval genre mix of engine top-50 for fixed seed sets."""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from collections import Counter
from collections.abc import Callable, Sequence
from pathlib import Path
from typing import Any

import psycopg2
from dotenv import load_dotenv

from genre_vocab import genre_name
from tmdb_source import TMDB_API_HOST, create_session

RESULT_LIMIT = 50
ANIMATION_ID = 16
HTTP_TIMEOUT_S = 10
TMDB_TIMEOUT_S = 10.0
DEFAULT_ENGINE_BASE_URL = "http://localhost:8181"
SEED_SETS_PATH = Path(__file__).resolve().parent / "eval_seed_sets.json"

CATALOG_META_SQL = """
SELECT tmdb_id, media_type, genre_ids, vote_count
FROM catalog_embedding
"""

CatalogRow = tuple[list[int] | None, int | None]


def normalize_title(title: str) -> str:
    """Keep letters and digits, lowercase."""
    return "".join(ch.lower() for ch in title if ch.isalnum())


def titles_match(expected: str, actual: str) -> bool:
    """True when normalized titles are equal."""
    return normalize_title(expected) == normalize_title(actual)


def collect_validation_problems(
    sets: Sequence[dict[str, Any]],
    catalog_keys: set[tuple[int, str]],
    tmdb_titles: dict[tuple[int, str], str | None],
) -> list[str]:
    """Collect catalog and title problems for every seed."""
    problems: list[str] = []
    for seed_set in sets:
        name = str(seed_set["name"])
        media_type = str(seed_set["media_type"])
        for seed in seed_set["seeds"]:
            tmdb_id = int(seed["tmdb_id"])
            expected = str(seed["title"])
            key = (tmdb_id, media_type)
            if key not in catalog_keys:
                problems.append(
                    f"{name} {tmdb_id} expected={expected} got=not in catalog"
                )
                continue
            actual = tmdb_titles.get(key)
            if actual is None:
                problems.append(
                    f"{name} {tmdb_id} expected={expected} got=TMDB fetch failed"
                )
                continue
            if not titles_match(expected, actual):
                problems.append(
                    f"{name} {tmdb_id} expected={expected} got={actual}"
                )
    return problems


def animation_stats(genre_id_rows: Sequence[list[int] | None]) -> tuple[int, float]:
    """Count rows with Animation (16) and percent of rows."""
    total = len(genre_id_rows)
    if total == 0:
        return 0, 0.0
    count = 0
    for genres in genre_id_rows:
        if genres and ANIMATION_ID in genres:
            count += 1
    return count, (100.0 * count) / total


def genre_histogram(
    genre_name_rows: Sequence[list[str]],
    limit: int = 10,
) -> list[tuple[str, int]]:
    """Top genre names by count desc, then name asc."""
    counts: Counter[str] = Counter()
    for names in genre_name_rows:
        counts.update(names)
    ranked = sorted(counts.items(), key=lambda item: (-item[1], item[0]))
    return ranked[:limit]


def median_vote_count(vote_counts: Sequence[int | None]) -> int | None:
    """Median of non-null vote counts, or None when empty."""
    values = sorted(int(v) for v in vote_counts if v is not None)
    if not values:
        return None
    mid = len(values) // 2
    if len(values) % 2 == 1:
        return values[mid]
    return (values[mid - 1] + values[mid]) // 2


def format_result_line(
    rank: int,
    title: str,
    media_type: str,
    genre_names: Sequence[str],
    vote_count: int | None,
    score: float,
) -> str:
    """One result line for stdout."""
    genres = ", ".join(genre_names)
    vote = "" if vote_count is None else str(vote_count)
    return f"{rank}\t{title}\t{media_type}\t{genres}\t{vote}\t{score:.4f}"


def format_seed_line(title: str, tmdb_id: int, vote_count: int | None) -> str:
    """One resolved seed line for stdout."""
    vote = "" if vote_count is None else str(vote_count)
    return f"{title}\t{tmdb_id}\t{vote}"


def build_rec_update_payload(
    loved: Sequence[tuple[int, str]],
    media_type: str,
    limit: int,
) -> dict[str, Any]:
    """Build the /rec/update JSON body with top-level media_type."""
    return {
        "loved": [
            {"tmdb_id": tmdb_id, "media_type": item_type}
            for tmdb_id, item_type in loved
        ],
        "media_type": media_type,
        "limit": limit,
    }


def count_media_type_mismatch(
    result_media_types: Sequence[str],
    expected_media_type: str,
) -> int:
    """Count result rows whose media type differs from the set."""
    return sum(1 for media_type in result_media_types if media_type != expected_media_type)


def names_for_genre_ids(genre_ids: list[int] | None) -> list[str]:
    """Map genre ids to names. NULL or unknown ids yield no crash."""
    if not genre_ids:
        return []
    names: list[str] = []
    for gid in genre_ids:
        try:
            names.append(genre_name(int(gid)))
        except KeyError:
            names.append(str(gid))
    return names


def cached_title(
    cache: dict[tuple[str, int], str | None],
    media_type: str,
    tmdb_id: int,
    fetch_fn: Callable[[str, int], str | None],
) -> str:
    """Return cached title, fetch once per id, or '?' on failure."""
    key = (media_type, tmdb_id)
    if key not in cache:
        cache[key] = fetch_fn(media_type, tmdb_id)
    value = cache[key]
    if value is None or not str(value).strip():
        return "?"
    return str(value)


def _retry_after_seconds(headers: Any) -> float:
    """Read Retry-After seconds, or 1 when missing."""
    raw = None if headers is None else headers.get("Retry-After")
    if raw is not None and str(raw).strip().isdigit():
        return float(raw)
    return 1.0


def fetch_tmdb_title(
    session: Any,
    media_type: str,
    tmdb_id: int,
    *,
    sleep: Callable[[float], None] = time.sleep,
) -> str | None:
    """GET movie title or tv name. One 429 retry. No matching importable fetch."""
    if media_type == "MOVIE":
        path = f"/3/movie/{tmdb_id}"
        field = "title"
    elif media_type == "TV":
        path = f"/3/tv/{tmdb_id}"
        field = "name"
    else:
        return None
    url = f"{TMDB_API_HOST}{path}"
    for attempt in range(2):
        try:
            response = session.get(url, timeout=TMDB_TIMEOUT_S)
        except Exception:
            return None
        status = int(response.status_code)
        if status == 200:
            try:
                payload = response.json()
            except Exception:
                return None
            value = payload.get(field)
            if value is None:
                return None
            return str(value)
        if status == 429 and attempt == 0:
            sleep(_retry_after_seconds(response.headers))
            continue
        return None
    return None


def read_env() -> dict[str, str]:
    """Load DB/TMDB from dotenv; UPDATE_TOKEN from process env only."""
    process_token = os.environ.get("UPDATE_TOKEN")
    if process_token is None or not process_token.strip():
        raise SystemExit("Missing required environment variable: UPDATE_TOKEN")
    update_token = process_token.strip()

    load_dotenv()
    values: dict[str, str] = {"UPDATE_TOKEN": update_token}
    for name in ("TMDB_API_KEY", "REC_WRITE_DATABASE_URL"):
        raw = os.environ.get(name)
        if raw is None or not raw.strip():
            raise SystemExit(f"Missing required environment variable: {name}")
        values[name] = raw.strip()

    engine = os.environ.get("REC_ENGINE_BASE_URL")
    values["REC_ENGINE_BASE_URL"] = (
        engine.strip() if engine and engine.strip() else DEFAULT_ENGINE_BASE_URL
    )
    return values


def load_seed_sets(path: Path = SEED_SETS_PATH) -> list[dict[str, Any]]:
    """Load the fixed eval seed sets JSON."""
    with path.open(encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, list):
        raise SystemExit("eval_seed_sets.json must be a list")
    return data


def load_catalog_meta(dsn: str) -> dict[tuple[int, str], CatalogRow]:
    """Read genre_ids and vote_count for every catalog row."""
    conn = psycopg2.connect(dsn)
    try:
        conn.set_session(readonly=True)
        with conn.cursor() as cur:
            cur.execute(CATALOG_META_SQL)
            rows: dict[tuple[int, str], CatalogRow] = {}
            for tmdb_id, media_type, genre_ids, vote_count in cur.fetchall():
                key = (int(tmdb_id), str(media_type))
                genres: list[int] | None
                if genre_ids is None:
                    genres = None
                else:
                    genres = [int(g) for g in genre_ids]
                vote = None if vote_count is None else int(vote_count)
                rows[key] = (genres, vote)
            return rows
    finally:
        conn.close()


def call_rec_update(
    engine_base_url: str,
    update_token: str,
    loved: Sequence[tuple[int, str]],
    media_type: str,
    limit: int = RESULT_LIMIT,
) -> list[dict[str, Any]]:
    """POST /rec/update. On HTTP error print status and body, exit 3."""
    url = engine_base_url.rstrip("/") + "/rec/update"
    payload = build_rec_update_payload(loved, media_type, limit)
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {update_token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT_S) as response:
            status = response.getcode()
            raw = response.read()
    except urllib.error.HTTPError as exc:
        err_body = exc.read()
        text = err_body.decode("utf-8", errors="replace")[:300]
        print(f"engine HTTP {exc.code}: {text}")
        raise SystemExit(3) from exc
    except urllib.error.URLError as exc:
        print(f"engine unreachable: {getattr(exc, 'reason', exc)}")
        raise SystemExit(3) from exc
    except TimeoutError as exc:
        print("engine HTTP timeout")
        raise SystemExit(3) from exc

    if status != 200:
        text = raw.decode("utf-8", errors="replace")[:300]
        print(f"engine HTTP {status}: {text}")
        raise SystemExit(3)

    try:
        data = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        print(f"engine HTTP {status}: invalid JSON body")
        raise SystemExit(3) from exc
    if not isinstance(data, list):
        print(f"engine HTTP {status}: body is not a list")
        raise SystemExit(3)
    return data


def format_histogram_line(items: Sequence[tuple[str, int]]) -> str:
    """Build the genre_histogram output line."""
    parts = [f"{name} {count}" for name, count in items]
    return "genre_histogram: " + ", ".join(parts)


def format_summary_table(
    rows: Sequence[tuple[str, int, int | None]],
    titles_missing: int,
) -> list[str]:
    """Build the final summary lines including titles_missing."""
    lines = ["summary"]
    for name, animation_count, median in rows:
        median_text = "" if median is None else str(median)
        lines.append(f"{name}\t{animation_count}\t{median_text}")
    lines.append(f"titles_missing: {titles_missing}")
    return lines


def main() -> None:
    """Validate seeds, call the engine per set, print genre-mix stats."""
    sys.stdout.reconfigure(encoding="utf-8")
    env = read_env()
    seed_sets = load_seed_sets()
    catalog = load_catalog_meta(env["REC_WRITE_DATABASE_URL"])
    catalog_keys = set(catalog)

    session = create_session(env["TMDB_API_KEY"])
    title_cache: dict[tuple[str, int], str | None] = {}

    def fetch_fn(media_type: str, tmdb_id: int) -> str | None:
        return fetch_tmdb_title(session, media_type, tmdb_id)

    tmdb_titles: dict[tuple[int, str], str | None] = {}
    for seed_set in seed_sets:
        media_type = str(seed_set["media_type"])
        for seed in seed_set["seeds"]:
            tmdb_id = int(seed["tmdb_id"])
            key = (tmdb_id, media_type)
            if key not in catalog_keys:
                continue
            title = cached_title(title_cache, media_type, tmdb_id, fetch_fn)
            if title == "?":
                tmdb_titles[key] = None
            else:
                tmdb_titles[key] = title

    problems = collect_validation_problems(seed_sets, catalog_keys, tmdb_titles)
    if problems:
        for problem in problems:
            print(problem)
        raise SystemExit(2)

    summary_rows: list[tuple[str, int, int | None]] = []
    titles_missing = 0
    out_lines: list[str] = []

    for seed_set in seed_sets:
        name = str(seed_set["name"])
        media_type = str(seed_set["media_type"])
        out_lines.append(name)

        loved: list[tuple[int, str]] = []
        for seed in seed_set["seeds"]:
            tmdb_id = int(seed["tmdb_id"])
            title = str(seed["title"])
            _genres, vote_count = catalog[(tmdb_id, media_type)]
            out_lines.append(format_seed_line(title, tmdb_id, vote_count))
            loved.append((tmdb_id, media_type))

        hits = call_rec_update(
            env["REC_ENGINE_BASE_URL"],
            env["UPDATE_TOKEN"],
            loved,
            media_type,
            RESULT_LIMIT,
        )

        genre_id_rows: list[list[int] | None] = []
        genre_name_rows: list[list[str]] = []
        vote_rows: list[int | None] = []
        hit_media_types: list[str] = []

        for rank, hit in enumerate(hits, start=1):
            hit_id = int(hit["tmdb_id"])
            hit_type = str(hit["media_type"])
            hit_media_types.append(hit_type)
            score = float(hit["score"])
            title = cached_title(title_cache, hit_type, hit_id, fetch_fn)
            if title == "?":
                titles_missing += 1
            meta = catalog.get((hit_id, hit_type))
            if meta is None:
                genres = None
                vote_count = None
            else:
                genres, vote_count = meta
            names = names_for_genre_ids(genres)
            genre_id_rows.append(genres)
            genre_name_rows.append(names)
            vote_rows.append(vote_count)
            out_lines.append(
                format_result_line(rank, title, hit_type, names, vote_count, score)
            )

        anim_count, anim_pct = animation_stats(genre_id_rows)
        median = median_vote_count(vote_rows)
        mismatch = count_media_type_mismatch(hit_media_types, media_type)
        out_lines.append(f"animation_in_top50: {anim_count} ({anim_pct:.1f}%)")
        out_lines.append(format_histogram_line(genre_histogram(genre_name_rows)))
        median_text = "" if median is None else str(median)
        out_lines.append(f"median_vote_count: {median_text}")
        out_lines.append(f"media_type_mismatch: {mismatch}")
        summary_rows.append((name, anim_count, median))

    out_lines.extend(format_summary_table(summary_rows, titles_missing))
    sys.stdout.write("\n".join(out_lines) + "\n")


if __name__ == "__main__":
    main()
