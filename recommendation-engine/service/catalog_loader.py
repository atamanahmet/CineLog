"""Load a CatalogSnapshot from catalog_embedding over PostgreSQL."""

from __future__ import annotations

import logging
import time

import numpy as np
import psycopg2
import psycopg2.errors

from catalog import CatalogSnapshot
from config import (
    DB_CONNECT_RETRIES,
    DB_CONNECT_TIMEOUT_SECONDS,
    EMBEDDING_DIM,
    EMBEDDING_MODEL_ID,
    MEDIA_TYPES,
    NORM_TOLERANCE,
    VECTOR_BYTES,
)

logger = logging.getLogger(__name__)

_LOAD_SQL = """
SELECT tmdb_id, media_type, vector, genre_ids
FROM catalog_embedding
WHERE model = %s
ORDER BY tmdb_id, media_type
"""

_MISSING_GENRE_IDS_MSG = (
    "catalog_embedding.genre_ids missing; apply "
    "embedding-script/sql/002_catalog_embedding_genres.sql before starting"
)


class EmptyCatalogError(RuntimeError):
    """catalog_embedding has no rows for the current model yet."""


def decode_vector(vector: bytes | memoryview, tmdb_id: int) -> np.ndarray:
    """Decode one float16 bytea row into a float32 embedding vector."""
    nbytes = len(vector)
    if nbytes != VECTOR_BYTES:
        raise ValueError(
            f"vector byte length mismatch for tmdb_id={tmdb_id}: "
            f"got {nbytes}, expected {VECTOR_BYTES}"
        )
    return np.frombuffer(vector, dtype=np.float16).astype(np.float32, copy=True)


def check_norms(
    matrix: np.ndarray,
    tmdb_ids: np.ndarray,
    tolerance: float = NORM_TOLERANCE,
) -> None:
    """Raise if any value is non-finite or any row L2 norm is off unit."""
    finite_ok = np.isfinite(matrix).all(axis=1)
    bad_finite = np.flatnonzero(~finite_ok)
    if bad_finite.size:
        row = int(bad_finite[0])
        raise ValueError(
            f"embedding has non-finite values at tmdb_id={int(tmdb_ids[row])}"
        )

    norms = np.linalg.norm(matrix, axis=1)
    bad_norm = np.flatnonzero(np.abs(norms - 1.0) > tolerance)
    if bad_norm.size:
        row = int(bad_norm[0])
        raise ValueError(
            f"embedding L2 norm out of tolerance at tmdb_id={int(tmdb_ids[row])}: "
            f"norm={float(norms[row])}, expected 1 +/- {tolerance}"
        )


def build_genre_membership(
    genre_lists: list[list[int]],
) -> tuple[np.ndarray, dict[int, int]]:
    """Build a bool matrix [rows, distinct genres] plus id-to-column map."""
    all_ids: set[int] = set()
    for genres in genre_lists:
        all_ids.update(genres)
    sorted_ids = sorted(all_ids)
    genre_col_index = {gid: col for col, gid in enumerate(sorted_ids)}
    matrix = np.zeros((len(genre_lists), len(sorted_ids)), dtype=bool)
    for row_i, genres in enumerate(genre_lists):
        for gid in genres:
            matrix[row_i, genre_col_index[gid]] = True
    return matrix, genre_col_index


def _normalize_genre_ids(raw) -> list[int]:
    """Treat NULL genre_ids as an empty membership set."""
    if raw is None:
        return []
    return [int(g) for g in raw]


def _connect(database_url: str):
    delay = 1.0
    last_error: BaseException | None = None
    for attempt in range(DB_CONNECT_RETRIES + 1):
        try:
            return psycopg2.connect(
                database_url,
                connect_timeout=DB_CONNECT_TIMEOUT_SECONDS,
            )
        except psycopg2.OperationalError as exc:
            last_error = exc
            if attempt >= DB_CONNECT_RETRIES:
                break
            logger.warning(
                "database connect failed (attempt %s/%s), retrying in %ss: %s",
                attempt + 1,
                DB_CONNECT_RETRIES + 1,
                delay,
                type(exc).__name__,
            )
            time.sleep(delay)
            delay *= 2
    raise last_error


def load_snapshot(database_url: str) -> CatalogSnapshot:
    started = time.monotonic()
    conn = _connect(database_url)
    try:
        # Before any statement: one snapshot for count + stream (see set_session docs).
        conn.set_session(isolation_level="REPEATABLE READ", readonly=True)
        with conn.cursor() as cur:
            cur.execute(
                "SELECT count(*) FROM catalog_embedding WHERE model = %s",
                (EMBEDDING_MODEL_ID,),
            )
            count = int(cur.fetchone()[0])
        if count == 0:
            raise EmptyCatalogError(
                f"catalog_embedding has zero rows for model={EMBEDDING_MODEL_ID}"
            )

        matrix = np.empty((count, EMBEDDING_DIM), dtype=np.float32)
        tmdb_ids = np.empty(count, dtype=np.int64)
        media_types = np.empty(count, dtype=object)
        genre_lists: list[list[int]] = [[] for _ in range(count)]
        index: dict[tuple[int, str], int] = {}
        row_i = 0
        null_genre_rows = 0

        with conn.cursor(name="catalog_embedding_load") as cur:
            cur.itersize = 2000
            try:
                cur.execute(_LOAD_SQL, (EMBEDDING_MODEL_ID,))
            except psycopg2.errors.UndefinedColumn as exc:
                raise RuntimeError(_MISSING_GENRE_IDS_MSG) from exc
            for tmdb_id, media_type, vector, genre_ids in cur:
                media_type_str = str(media_type)
                if media_type_str not in MEDIA_TYPES:
                    raise ValueError(
                        f"unsupported media_type={media_type_str!r} "
                        f"for tmdb_id={int(tmdb_id)}"
                    )
                decoded = decode_vector(vector, int(tmdb_id))
                matrix[row_i] = decoded
                tmdb_ids[row_i] = int(tmdb_id)
                media_types[row_i] = media_type_str
                if genre_ids is None:
                    null_genre_rows += 1
                genre_lists[row_i] = _normalize_genre_ids(genre_ids)
                index[(int(tmdb_id), media_type_str)] = row_i
                row_i += 1

        if row_i != count:
            raise RuntimeError("row count changed during load")

        check_norms(matrix, tmdb_ids)
        genre_matrix, genre_col_index = build_genre_membership(genre_lists)
        snapshot = CatalogSnapshot(
            tmdb_ids=tmdb_ids,
            media_types=media_types,
            matrix=matrix,
            index=index,
            genre_matrix=genre_matrix,
            genre_col_index=genre_col_index,
        )
        elapsed = time.monotonic() - started
        logger.info(
            "catalog snapshot loaded rows=%s genre_null_rows=%s duration_s=%.3f",
            count,
            null_genre_rows,
            elapsed,
        )
        return snapshot
    finally:
        conn.close()
