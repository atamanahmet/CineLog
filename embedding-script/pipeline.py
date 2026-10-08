"""Fetch TMDB titles, embed the new ones and write them to the rec database."""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from enum import Enum

from catalog_repository import EmbeddingRow, Key, read_stored_keys, write_embeddings
from config import Settings
from db_connection import open_connection
from embedder import EMBEDDING_MODEL_ID, build_text, encode_texts, load_model
from genre_vocab import to_canonical
from tmdb_source import (
    MEDIA_TYPES,
    MIN_REQUEST_INTERVAL_S,
    CatalogItem,
    RequestThrottle,
    TmdbRequestError,
    create_session,
    iter_discover_pages,
)

CHUNK_SIZE = 1000

logger = logging.getLogger(__name__)


class SkipReason(Enum):
    STORED = "stored"
    EMPTY = "empty"
    DUPLICATE = "duplicate"


@dataclass(frozen=True)
class RunResult:
    written: int
    tmdb_failed: bool


def skip_reason(
    item: CatalogItem,
    stored_keys: set[Key],
    seen_keys: set[Key],
) -> SkipReason | None:
    """Return why an item is skipped, or None to keep it."""
    key = (item.tmdb_id, item.media_type)
    if key in stored_keys:
        return SkipReason.STORED
    if item.overview is None or not str(item.overview).strip():
        return SkipReason.EMPTY
    if key in seen_keys:
        return SkipReason.DUPLICATE
    return None


class ChunkBuffer:
    """Pending items for one flush cycle, with skip counters."""

    def __init__(self, stored_keys: set[Key]) -> None:
        self._stored_keys = stored_keys
        self._seen_keys: set[Key] = set()
        self.items: list[CatalogItem] = []
        self.skipped_stored = 0
        self.skipped_empty = 0

    def add(self, item: CatalogItem) -> None:
        """Keep the item unless it is stored, empty or a duplicate."""
        reason = skip_reason(item, self._stored_keys, self._seen_keys)
        if reason is SkipReason.STORED:
            self.skipped_stored += 1
        elif reason is SkipReason.EMPTY:
            self.skipped_empty += 1
        elif reason is None:
            self._seen_keys.add((item.tmdb_id, item.media_type))
            self.items.append(item)

    def is_full(self) -> bool:
        return len(self.items) >= CHUNK_SIZE

    def clear(self) -> None:
        """Reset items and counters. Seen keys stay, so duplicates stay skipped."""
        self.items = []
        self.skipped_stored = 0
        self.skipped_empty = 0


def _to_row(item: CatalogItem, vector: bytes) -> EmbeddingRow:
    """Build one DB row. Genre ids are mapped to the canonical vocabulary."""
    genre_ids = (
        None
        if item.genre_ids is None
        else to_canonical(item.media_type, item.genre_ids)
    )
    return EmbeddingRow(
        item.tmdb_id, item.media_type, vector, genre_ids, item.vote_count
    )


def _flush(settings: Settings, model, buffer: ChunkBuffer) -> int:
    """Embed and write the buffered items, then clear the buffer. Returns rows written."""
    if not buffer.items:
        return 0
    texts = [build_text(item.title, str(item.overview)) for item in buffer.items]
    vectors = encode_texts(model, texts)
    rows = [_to_row(item, vector) for item, vector in zip(buffer.items, vectors)]
    with open_connection(settings.database_url, settings.db_retry) as conn:
        write_embeddings(conn, rows, EMBEDDING_MODEL_ID)
    logger.info(
        "rows written=%s media_type=%s skipped_empty_overview=%s "
        "skipped_already_stored=%s",
        len(rows),
        buffer.items[0].media_type,
        buffer.skipped_empty,
        buffer.skipped_stored,
    )
    buffer.clear()
    return len(rows)


def run_embedding(settings: Settings, pages: int) -> RunResult:
    """Embed every new discover title. The DB is opened only when it is needed."""
    with open_connection(settings.database_url, settings.db_retry) as conn:
        stored_keys = read_stored_keys(conn, EMBEDDING_MODEL_ID)
    model = load_model()
    session = create_session(settings.tmdb_api_key)
    throttle = RequestThrottle(MIN_REQUEST_INTERVAL_S, time.monotonic, time.sleep)
    buffer = ChunkBuffer(stored_keys)
    written = 0
    tmdb_failed = False

    try:
        for media_type in MEDIA_TYPES:
            for page_items in iter_discover_pages(session, media_type, pages, throttle):
                for item in page_items:
                    buffer.add(item)
                    if buffer.is_full():
                        written += _flush(settings, model, buffer)
            written += _flush(settings, model, buffer)
    except TmdbRequestError:
        logger.exception("TMDB fetch failed")
        tmdb_failed = True

    written += _flush(settings, model, buffer)
    return RunResult(written=written, tmdb_failed=tmdb_failed)
