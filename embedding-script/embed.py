"""Embed TMDB discover titles into catalog_embedding, then ping reload."""

from __future__ import annotations

import argparse
import logging
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any

import numpy as np
import psycopg2
from dotenv import load_dotenv
from psycopg2.extras import execute_values

from genre_vocab import to_canonical
from tmdb_source import (
    MIN_REQUEST_INTERVAL_S,
    CatalogItem,
    RequestThrottle,
    TmdbRequestError,
    create_session,
    iter_discover_pages,
)

MODEL_NAME = "BAAI/bge-small-en-v1.5"
EMBEDDING_MODEL_ID = 1
BATCH_SIZE = 64
CHUNK_SIZE = 1000
MEDIA_TYPES = ("MOVIE", "TV")

REQUIRED_ENV = (
    "TMDB_API_KEY",
    "REC_WRITE_DATABASE_URL",
    "RELOAD_URL",
    "RELOAD_TOKEN",
)

STORED_KEYS_SQL = """
SELECT tmdb_id, media_type
FROM catalog_embedding
WHERE model = %s
"""

UPSERT_SQL = """
INSERT INTO catalog_embedding (
    tmdb_id, media_type, vector, model, updated_at, genre_ids, vote_count
)
VALUES %s
ON CONFLICT (tmdb_id, media_type) DO UPDATE SET
    vector = EXCLUDED.vector,
    model = EXCLUDED.model,
    updated_at = now()
"""

UPSERT_TEMPLATE = "(%s, %s, %s, %s, now(), %s, %s)"

logger = logging.getLogger(__name__)

Key = tuple[int, str]


class ChunkBuffer:
    """Pending catalog items and skip counters for one flush cycle."""

    def __init__(self) -> None:
        self.items: list[CatalogItem] = []
        self.skipped_stored = 0
        self.skipped_empty = 0

    def clear(self) -> None:
        self.items = []
        self.skipped_stored = 0
        self.skipped_empty = 0


def require_env() -> dict[str, str]:
    load_dotenv()
    values: dict[str, str] = {}
    for name in REQUIRED_ENV:
        raw = os.environ.get(name)
        if raw is None or not raw.strip():
            raise SystemExit(f"Missing required environment variable: {name}")
        values[name] = raw.strip()
    return values


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Embed TMDB discover pages")
    parser.add_argument(
        "--pages",
        type=int,
        default=500,
        help="Discover pages per media type (1-500)",
    )
    args = parser.parse_args(argv)
    if not 1 <= args.pages <= 500:
        parser.error("--pages must be between 1 and 500")
    return args


def build_text(title: str, overview: str) -> str:
    return f"{title}. {overview}"


def skip_reason(
    item: CatalogItem,
    stored_keys: set[Key],
    seen_keys: set[Key],
) -> str | None:
    """Return why an item is skipped, or None to keep it."""
    key = (item.tmdb_id, item.media_type)
    if key in stored_keys:
        return "stored"
    if item.overview is None or not str(item.overview).strip():
        return "empty"
    if key in seen_keys:
        return "duplicate"
    return None


def to_float16_bytes(vector: Any) -> bytes:
    return np.asarray(vector, dtype=np.float16).tobytes()


def embedding_row_fields(item: CatalogItem) -> tuple[list[int] | None, int | None]:
    """Build canonical genre_ids and vote_count for a new embedding row."""
    if item.genre_ids is None:
        genres: list[int] | None = None
    else:
        genres = to_canonical(item.media_type, item.genre_ids)
    return genres, item.vote_count


def read_stored_keys(conn, model_id: int) -> set[Key]:
    with conn.cursor() as cur:
        cur.execute(STORED_KEYS_SQL, (model_id,))
        return {(int(tmdb_id), str(media_type)) for tmdb_id, media_type in cur.fetchall()}


def load_model():
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(MODEL_NAME)


def encode_texts(model, texts: list[str]) -> list[bytes]:
    started = time.monotonic()
    embeddings = model.encode(
        texts,
        batch_size=BATCH_SIZE,
        normalize_embeddings=True,
    )
    duration = time.monotonic() - started
    logger.info("encode duration_s=%.3f", duration)
    return [to_float16_bytes(row) for row in embeddings]


def write_embeddings(
    conn,
    rows: list[tuple[int, str, bytes, list[int] | None, int | None]],
    model_id: int,
) -> None:
    """Insert embedding rows. Conflict updates vector fields only."""
    with conn.cursor() as cur:
        execute_values(
            cur,
            UPSERT_SQL,
            [
                (
                    tmdb_id,
                    media_type,
                    psycopg2.Binary(vector),
                    model_id,
                    genre_ids,
                    vote_count,
                )
                for tmdb_id, media_type, vector, genre_ids, vote_count in rows
            ],
            template=UPSERT_TEMPLATE,
        )
    conn.commit()


def ping_reload(url: str, token: str) -> int | None:
    """POST reload. Returns HTTP status or None on transport failure."""
    req = urllib.request.Request(
        url,
        data=b"",
        method="POST",
        headers={"Authorization": f"Bearer {token}"},
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return int(resp.status)
    except urllib.error.HTTPError as exc:
        return int(exc.code)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        logger.warning("ping transport failed: %s", type(exc).__name__)
        return None


def flush_chunk(
    conn,
    model,
    chunk: list[CatalogItem],
    skipped_stored: int,
    skipped_empty: int,
) -> int:
    """Encode and write one chunk. Returns rows written."""
    if not chunk:
        return 0
    texts = [build_text(item.title, str(item.overview)) for item in chunk]
    vectors = encode_texts(model, texts)
    write_rows = []
    for item, vector in zip(chunk, vectors):
        genre_ids, vote_count = embedding_row_fields(item)
        write_rows.append(
            (item.tmdb_id, item.media_type, vector, genre_ids, vote_count)
        )
    write_embeddings(conn, write_rows, EMBEDDING_MODEL_ID)
    written = len(write_rows)
    logger.info(
        "rows written=%s media_type=%s skipped_empty_overview=%s "
        "skipped_already_stored=%s",
        written,
        chunk[0].media_type,
        skipped_empty,
        skipped_stored,
    )
    return written


def flush_pending(conn, model, buffer: ChunkBuffer) -> int:
    """Flush buffer items to the DB and reset counters. Returns rows written."""
    if not buffer.items:
        return 0
    written = flush_chunk(
        conn,
        model,
        buffer.items,
        buffer.skipped_stored,
        buffer.skipped_empty,
    )
    buffer.clear()
    return written


def main(argv: list[str] | None = None) -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    args = parse_args(argv)
    env = require_env()

    rec_conn = None
    total_written = 0
    tmdb_failed = False
    try:
        rec_conn = psycopg2.connect(env["REC_WRITE_DATABASE_URL"])
        stored_keys = read_stored_keys(rec_conn, EMBEDDING_MODEL_ID)
        model = load_model()
        session = create_session(env["TMDB_API_KEY"])
        throttle = RequestThrottle(MIN_REQUEST_INTERVAL_S, time.monotonic, time.sleep)

        buffer = ChunkBuffer()
        seen_keys: set[Key] = set()

        try:
            for media_type in MEDIA_TYPES:
                for page_items in iter_discover_pages(
                    session,
                    media_type,
                    args.pages,
                    throttle,
                ):
                    for item in page_items:
                        reason = skip_reason(item, stored_keys, seen_keys)
                        if reason == "stored":
                            buffer.skipped_stored += 1
                            continue
                        if reason == "empty":
                            buffer.skipped_empty += 1
                            continue
                        if reason == "duplicate":
                            continue
                        seen_keys.add((item.tmdb_id, item.media_type))
                        buffer.items.append(item)
                        if len(buffer.items) >= CHUNK_SIZE:
                            total_written += flush_pending(rec_conn, model, buffer)
                total_written += flush_pending(rec_conn, model, buffer)
        except TmdbRequestError:
            logger.exception("TMDB fetch failed")
            tmdb_failed = True

        if total_written == 0:
            logger.info("nothing to embed")

        status = ping_reload(env["RELOAD_URL"], env["RELOAD_TOKEN"])
        if status is not None and 200 <= status < 300:
            logger.info("ping result=ok status=%s", status)
        else:
            logger.warning("ping result=failed status=%s", status)

        logger.info(
            "done total_written=%s tmdb_failed=%s",
            total_written,
            tmdb_failed,
        )
    finally:
        if rec_conn is not None:
            rec_conn.close()

    if tmdb_failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
