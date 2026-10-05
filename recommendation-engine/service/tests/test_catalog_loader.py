import os
import threading

os.environ.setdefault("REC_DATABASE_URL", "postgres://test")
os.environ.setdefault("UPDATE_TOKEN", "test-update-token-xxxxxxxxxxxxxx")
os.environ.setdefault("RELOAD_TOKEN", "test-reload-token-xxxxxxxxxxxxxx")

import numpy as np
import pytest

from catalog import CatalogSnapshot, empty_genre_membership
from catalog_loader import check_norms, decode_vector
from catalog_store import CatalogStore, ReloadInProgress
from config import EMBEDDING_DIM, NORM_TOLERANCE, VECTOR_BYTES


def _unit_vector(seed: int = 0) -> np.ndarray:
    rng = np.random.default_rng(seed)
    vec = rng.standard_normal(EMBEDDING_DIM).astype(np.float32)
    return vec / np.linalg.norm(vec)


def _float16_bytes(vec: np.ndarray) -> bytes:
    return np.asarray(vec, dtype=np.float16).tobytes()


def _snapshot_from_matrix(matrix: np.ndarray) -> CatalogSnapshot:
    rows = matrix.shape[0]
    tmdb_ids = np.arange(rows, dtype=np.int64)
    media_types = np.array(["MOVIE"] * rows, dtype=object)
    index = {(int(tmdb_ids[i]), "MOVIE"): i for i in range(rows)}
    genre_matrix, genre_col_index = empty_genre_membership(rows)
    return CatalogSnapshot(
        tmdb_ids=tmdb_ids,
        media_types=media_types,
        matrix=matrix,
        index=index,
        genre_matrix=genre_matrix,
        genre_col_index=genre_col_index,
    )


def test_decode_vector_wrong_byte_length_raises():
    with pytest.raises(ValueError, match="tmdb_id=42"):
        decode_vector(b"\x00" * (VECTOR_BYTES - 2), 42)


def test_check_norms_nan_row_raises():
    matrix = np.stack([_unit_vector(i) for i in range(5)])
    tmdb_ids = np.array([10, 20, 30, 40, 50], dtype=np.int64)
    matrix[2, 0] = np.nan
    with pytest.raises(ValueError, match="non-finite values at tmdb_id=30"):
        check_norms(matrix, tmdb_ids, tolerance=NORM_TOLERANCE)


def test_check_norms_bad_norm_beyond_old_sample_size_raises():
    # Old checker sampled 20 rows; a bad row at index 24 could be missed.
    rows = 25
    matrix = np.stack([_unit_vector(i) for i in range(rows)])
    tmdb_ids = np.arange(1000, 1000 + rows, dtype=np.int64)
    matrix[24] = 0.0
    with pytest.raises(ValueError, match="tmdb_id=1024"):
        check_norms(matrix, tmdb_ids, tolerance=NORM_TOLERANCE)


def test_decode_vector_round_trip_within_1e_3():
    original = _unit_vector(7)
    decoded = decode_vector(_float16_bytes(original), tmdb_id=1)
    assert decoded.dtype == np.float32
    assert decoded.shape == (EMBEDDING_DIM,)
    assert float(np.max(np.abs(decoded - original))) < 1e-3


def test_snapshot_rejects_empty():
    with pytest.raises(ValueError, match="at least one row"):
        CatalogSnapshot(
            tmdb_ids=np.empty(0, dtype=np.int64),
            media_types=np.empty(0, dtype=object),
            matrix=np.empty((0, EMBEDDING_DIM), dtype=np.float32),
            index={},
            genre_matrix=np.empty((0, 0), dtype=bool),
            genre_col_index={},
        )


def test_snapshot_rejects_length_mismatch():
    matrix = _unit_vector(1).reshape(1, -1)
    genre_matrix, genre_col_index = empty_genre_membership(1)
    with pytest.raises(ValueError, match="length mismatch"):
        CatalogSnapshot(
            tmdb_ids=np.array([1, 2], dtype=np.int64),
            media_types=np.array(["MOVIE"], dtype=object),
            matrix=matrix,
            index={(1, "MOVIE"): 0},
            genre_matrix=genre_matrix,
            genre_col_index=genre_col_index,
        )


def test_snapshot_arrays_are_read_only():
    snap = _snapshot_from_matrix(_unit_vector(2).reshape(1, -1))
    assert not snap.matrix.flags.writeable
    assert not snap.tmdb_ids.flags.writeable
    assert not snap.media_types.flags.writeable
    assert not snap.genre_matrix.flags.writeable
    with pytest.raises(ValueError):
        snap.matrix[0, 0] = 0.0
    with pytest.raises(ValueError):
        snap.tmdb_ids[0] = 99
    with pytest.raises(ValueError):
        snap.media_types[0] = "TV"
    genre_snap = CatalogSnapshot(
        tmdb_ids=snap.tmdb_ids,
        media_types=snap.media_types,
        matrix=snap.matrix,
        index=snap.index,
        genre_matrix=np.zeros((1, 1), dtype=bool),
        genre_col_index={28: 0},
    )
    assert not genre_snap.genre_matrix.flags.writeable
    with pytest.raises(ValueError):
        genre_snap.genre_matrix[0, 0] = True


def test_reload_in_progress_when_lock_held():
    store = CatalogStore()
    started = threading.Event()
    release = threading.Event()

    def slow_loader() -> CatalogSnapshot:
        started.set()
        assert release.wait(timeout=5)
        return _snapshot_from_matrix(_unit_vector(3).reshape(1, -1))

    errors: list[BaseException] = []

    def run_slow() -> None:
        try:
            store.reload(slow_loader)
        except BaseException as exc:
            errors.append(exc)

    thread = threading.Thread(target=run_slow)
    thread.start()
    assert started.wait(timeout=5)
    with pytest.raises(ReloadInProgress):
        store.reload(lambda: _snapshot_from_matrix(_unit_vector(4).reshape(1, -1)))
    release.set()
    thread.join(timeout=5)
    assert not thread.is_alive()
    assert errors == []
    assert store.get() is not None
