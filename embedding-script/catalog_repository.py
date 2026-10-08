"""SQL access to the catalog_embedding table."""

from __future__ import annotations

from typing import NamedTuple

import psycopg2
from psycopg2.extensions import connection as PgConnection
from psycopg2.extras import execute_values

Key = tuple[int, str]

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


class EmbeddingRow(NamedTuple):
    tmdb_id: int
    media_type: str
    vector: bytes
    genre_ids: list[int] | None
    vote_count: int | None


def read_stored_keys(conn: PgConnection, model_id: int) -> set[Key]:
    """Return the keys that already have a vector for this model."""
    with conn.cursor() as cur:
        cur.execute(STORED_KEYS_SQL, (model_id,))
        return {(int(tmdb_id), str(media_type)) for tmdb_id, media_type in cur.fetchall()}


def write_embeddings(
    conn: PgConnection,
    rows: list[EmbeddingRow],
    model_id: int,
) -> None:
    """Upsert rows in one transaction. Conflict updates vector fields only."""
    with conn, conn.cursor() as cur:
        execute_values(
            cur,
            UPSERT_SQL,
            [
                (
                    row.tmdb_id,
                    row.media_type,
                    psycopg2.Binary(row.vector),
                    model_id,
                    row.genre_ids,
                    row.vote_count,
                )
                for row in rows
            ],
            template=UPSERT_TEMPLATE,
        )
