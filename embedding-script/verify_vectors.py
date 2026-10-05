"""Read-only check of stored embeddings."""

from __future__ import annotations

import sys

import numpy as np
import psycopg2

from embed import (
    EMBEDDING_MODEL_ID,
    MEDIA_TYPES,
    build_text,
    encode_texts,
    load_model,
    require_env,
)
from tmdb_source import create_session

EXPECTED_DIM = 384
NORM_TOLERANCE = 0.01
MIN_SAMPLE_COSINE = 0.99
SAMPLES_PER_TYPE = 3
TMDB_BASE_URL = "https://api.themoviedb.org/3"

ROWS_SQL = """
SELECT tmdb_id, media_type, vector
FROM catalog_embedding
WHERE model = %s
ORDER BY media_type, tmdb_id
"""


def decode(raw) -> np.ndarray:
    """Turn stored bytes into float32, same dtype the writer used."""
    return np.frombuffer(bytes(raw), dtype=np.float16).astype(np.float32)


def read_rows(dsn: str) -> list[tuple[int, str, np.ndarray]]:
    """Load all rows of the current model, decoded."""
    conn = psycopg2.connect(dsn)
    try:
        conn.set_session(readonly=True)
        with conn.cursor() as cur:
            cur.execute(ROWS_SQL, (EMBEDDING_MODEL_ID,))
            return [(int(i), str(t), decode(v)) for i, t, v in cur.fetchall()]
    finally:
        conn.close()


def check_vectors(rows: list[tuple[int, str, np.ndarray]]) -> list[str]:
    """Check dimension, finite values and norm. Returns failure messages."""
    failures: list[str] = []
    dims = sorted({vector.shape[0] for _, _, vector in rows})
    print(f"rows={len(rows)} dims={dims}")
    if dims != [EXPECTED_DIM]:
        return [f"dimension is {dims}, expected {EXPECTED_DIM}"]

    matrix = np.stack([vector for _, _, vector in rows])
    non_finite = int((~np.isfinite(matrix).all(axis=1)).sum())
    norms = np.linalg.norm(matrix, axis=1)
    print(f"non_finite_rows={non_finite}")
    print(f"norm min={norms.min():.4f} max={norms.max():.4f} mean={norms.mean():.4f}")

    if non_finite:
        failures.append(f"{non_finite} rows have NaN or Inf")
    if np.abs(norms - 1.0).max() > NORM_TOLERANCE:
        failures.append("some norms are not close to 1")
    return failures


def fetch_title(session, media_type: str, tmdb_id: int) -> tuple[str, str] | None:
    """Get title and overview from TMDB for the stored type."""
    path = "movie" if media_type == "MOVIE" else "tv"
    resp = session.get(f"{TMDB_BASE_URL}/{path}/{tmdb_id}", timeout=30)
    if resp.status_code != 200:
        return None
    data = resp.json()
    return (data.get("title") or data.get("name") or "", data.get("overview") or "")


def pick_samples(rows, media_type: str) -> list[tuple[int, str, np.ndarray]]:
    """Pick a fixed random sample so runs are repeatable."""
    typed = [row for row in rows if row[1] == media_type]
    rng = np.random.default_rng(0)
    indexes = rng.choice(len(typed), size=min(SAMPLES_PER_TYPE, len(typed)), replace=False)
    return [typed[i] for i in indexes]


def check_samples(rows, env: dict[str, str]) -> list[str]:
    """Re-embed sample titles and compare with the stored vectors."""
    failures: list[str] = []
    session = create_session(env["TMDB_API_KEY"])
    model = load_model()
    for media_type in MEDIA_TYPES:
        for tmdb_id, _, stored in pick_samples(rows, media_type):
            found = fetch_title(session, media_type, tmdb_id)
            if found is None:
                failures.append(f"{media_type} {tmdb_id} not found on TMDB")
                continue
            title, overview = found
            fresh = decode(encode_texts(model, [build_text(title, overview)])[0])
            cosine = float(np.dot(stored, fresh))
            print(f"{media_type} {tmdb_id} cosine={cosine:.4f} title={title}")
            if cosine < MIN_SAMPLE_COSINE:
                failures.append(f"{media_type} {tmdb_id} cosine {cosine:.4f} is low")
    return failures


def main() -> None:
    env = require_env()
    rows = read_rows(env["REC_WRITE_DATABASE_URL"])
    failures = check_vectors(rows)
    if not failures:
        failures = check_samples(rows, env)
    for message in failures:
        print(f"FAIL: {message}")
    print("RESULT: FAIL" if failures else "RESULT: PASS")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()