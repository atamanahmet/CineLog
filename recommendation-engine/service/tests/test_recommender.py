import os

os.environ.setdefault("REC_DATABASE_URL", "postgres://test")
os.environ.setdefault("UPDATE_TOKEN", "test-update-token-xxxxxxxxxxxxxx")
os.environ.setdefault("RELOAD_TOKEN", "test-reload-token-xxxxxxxxxxxxxx")
os.environ["SIMILARITY_THRESHOLD"] = "0.3"

import numpy as np

from catalog import CatalogSnapshot, empty_genre_membership
from catalog_loader import build_genre_membership
from recommender import GENRE_INCLUDE_MODE, build_genre_keep_mask, recommend


def _l2(rows: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(rows, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    return (rows / norms).astype(np.float32)


def _tiny_snapshot() -> CatalogSnapshot:
    """
    6 rows, 4 dims. Hand layout:
    0 MOVIE 10  [1, 0, 0, 0]
    1 MOVIE 20  [0.8, 0.6, 0, 0]   (will be L2-normalized)
    2 MOVIE 30  [0, 1, 0, 0]
    3 TV    40  [0.6, 0.8, 0, 0]
    4 TV    50  [0, 0, 1, 0]
    5 MOVIE 60  [0.05, 1, 0, 0]    low similarity to e0 after norm
    """
    raw = np.array(
        [
            [1.0, 0.0, 0.0, 0.0],
            [0.8, 0.6, 0.0, 0.0],
            [0.0, 1.0, 0.0, 0.0],
            [0.6, 0.8, 0.0, 0.0],
            [0.0, 0.0, 1.0, 0.0],
            [0.05, 1.0, 0.0, 0.0],
        ],
        dtype=np.float32,
    )
    matrix = _l2(raw)
    tmdb_ids = np.array([10, 20, 30, 40, 50, 60], dtype=np.int64)
    media_types = np.array(
        ["MOVIE", "MOVIE", "MOVIE", "TV", "TV", "MOVIE"], dtype=object
    )
    index = {
        (10, "MOVIE"): 0,
        (20, "MOVIE"): 1,
        (30, "MOVIE"): 2,
        (40, "TV"): 3,
        (50, "TV"): 4,
        (60, "MOVIE"): 5,
    }
    genre_matrix, genre_col_index = empty_genre_membership(6)
    return CatalogSnapshot(
        tmdb_ids=tmdb_ids,
        media_types=media_types,
        matrix=matrix,
        index=index,
        genre_matrix=genre_matrix,
        genre_col_index=genre_col_index,
    )


def test_loved_item_never_in_output():
    snap = _tiny_snapshot()
    out = recommend(snap, [(10, "MOVIE")], None, limit=10)
    keys = {(row["tmdb_id"], row["media_type"]) for row in out}
    assert (10, "MOVIE") not in keys
    assert all(isinstance(row["tmdb_id"], int) for row in out)
    assert all(isinstance(row["media_type"], str) for row in out)
    assert all(isinstance(row["score"], float) for row in out)
    assert all(type(row["score"]) is float for row in out)


def test_duplicate_loved_same_as_single():
    snap = _tiny_snapshot()
    once = recommend(snap, [(10, "MOVIE")], None, limit=10)
    twice = recommend(
        snap,
        [(10, "MOVIE"), (10, "MOVIE"), (10, "MOVIE")],
        None,
        limit=10,
    )
    assert once == twice


def test_unknown_loved_ids_skipped():
    snap = _tiny_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE"), (999, "MOVIE")],
        None,
        limit=10,
    )
    baseline = recommend(snap, [(10, "MOVIE")], None, limit=10)
    assert out == baseline


def test_all_unknown_loved_returns_empty():
    snap = _tiny_snapshot()
    assert recommend(snap, [(999, "MOVIE"), (998, "TV")], None, limit=5) == []


def test_media_type_filter_returns_only_that_type():
    snap = _tiny_snapshot()
    out = recommend(snap, [(10, "MOVIE")], "TV", limit=10)
    assert out
    assert all(row["media_type"] == "TV" for row in out)


def test_threshold_removes_low_scores():
    snap = _tiny_snapshot()
    out = recommend(snap, [(10, "MOVIE")], None, limit=10)
    scores = [row["score"] for row in out]
    assert scores
    assert all(score >= 0.3 for score in scores)
    ids = {row["tmdb_id"] for row in out}
    assert 50 not in ids
    assert 60 not in ids


def test_limit_respected_and_descending():
    snap = _tiny_snapshot()
    out = recommend(snap, [(10, "MOVIE")], None, limit=2)
    assert len(out) == 2
    assert out[0]["score"] >= out[1]["score"]


def test_hand_computed_top_item():
    snap = _tiny_snapshot()
    loved_vec = snap.matrix[0]
    expected_scores = snap.matrix @ loved_vec
    expected_top_row = int(np.argsort(-expected_scores)[1])
    assert expected_top_row == 1
    expected_score = float(expected_scores[1])
    out = recommend(snap, [(10, "MOVIE")], None, limit=1)
    assert len(out) == 1
    assert out[0]["tmdb_id"] == 20
    assert out[0]["media_type"] == "MOVIE"
    assert abs(out[0]["score"] - expected_score) < 1e-6


def test_min_score_filters_known_cosine_scores():
    # Loved row 0 [1,0,0,0]: 20→0.8, 40→0.6, 30→0.0 (default threshold 0.3)
    snap = _tiny_snapshot()
    default = recommend(snap, [(10, "MOVIE")], None, limit=10)
    default_ids = {row["tmdb_id"] for row in default}
    assert 20 in default_ids
    assert 40 in default_ids

    none_score = recommend(snap, [(10, "MOVIE")], None, limit=10, min_score=None)
    assert {row["tmdb_id"] for row in none_score} == default_ids

    high = recommend(snap, [(10, "MOVIE")], None, limit=10, min_score=0.7)
    high_ids = {row["tmdb_id"] for row in high}
    assert 20 in high_ids
    assert 40 not in high_ids


def test_no_exclude_same_output_as_today():
    snap = _tiny_snapshot()
    baseline = recommend(snap, [(10, "MOVIE")], None, limit=10)
    with_none = recommend(
        snap, [(10, "MOVIE")], None, limit=10, exclude_mask=None
    )
    zeros = np.zeros(snap.matrix.shape[0], dtype=bool)
    with_zeros = recommend(
        snap, [(10, "MOVIE")], None, limit=10, exclude_mask=zeros
    )
    assert with_none == baseline
    assert with_zeros == baseline


def test_exclude_unknown_id_ignored():
    from recommender import build_exclude_mask

    snap = _tiny_snapshot()
    baseline = recommend(snap, [(10, "MOVIE")], None, limit=10)
    mask = build_exclude_mask(
        [(999, "MOVIE")],
        snap.index,
        int(snap.matrix.shape[0]),
    )
    out = recommend(
        snap, [(10, "MOVIE")], None, limit=10, exclude_mask=mask
    )
    assert out == baseline


def _genre_snapshot() -> CatalogSnapshot:
    """
    Same vectors as tiny snapshot, with genre membership:
    10 MOVIE: NULL → empty
    20 MOVIE: [878, 28] sci-fi + action
    30 MOVIE: [16] animation
    40 TV:    [878] sci-fi
    50 TV:    [28] action
    60 MOVIE: [878, 16] sci-fi + animation
    """
    base = _tiny_snapshot()
    genre_lists = [
        [],
        [878, 28],
        [16],
        [878],
        [28],
        [878, 16],
    ]
    genre_matrix, genre_col_index = build_genre_membership(genre_lists)
    return CatalogSnapshot(
        tmdb_ids=base.tmdb_ids,
        media_types=base.media_types,
        matrix=base.matrix,
        index=base.index,
        genre_matrix=genre_matrix,
        genre_col_index=genre_col_index,
    )


def test_genre_include_mode_is_all():
    assert GENRE_INCLUDE_MODE == "all"


def test_genre_include_all_requires_every_id():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_include=[878, 28],
    )
    ids = {row["tmdb_id"] for row in out}
    assert ids == {20}
    assert all(row["tmdb_id"] != 10 for row in out)


def test_genre_exclude_drops_any_listed_id():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_exclude=[16],
    )
    ids = {row["tmdb_id"] for row in out}
    assert 30 not in ids
    assert 60 not in ids
    assert 20 in ids


def test_genre_include_and_exclude_together():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_include=[878],
        genre_exclude=[16],
    )
    ids = {row["tmdb_id"] for row in out}
    assert ids == {20, 40}


def test_genre_include_unknown_id_returns_empty():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_include=[999999],
    )
    assert out == []


def test_genre_exclude_unknown_id_no_effect():
    snap = _genre_snapshot()
    baseline = recommend(
        snap, [(10, "MOVIE")], None, limit=10, min_score=0
    )
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_exclude=[999999],
    )
    assert out == baseline


def test_null_genre_row_excluded_by_include_kept_by_exclude():
    snap = _genre_snapshot()
    with_include = recommend(
        snap,
        [(20, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_include=[878],
    )
    assert all(row["tmdb_id"] != 10 for row in with_include)

    with_exclude = recommend(
        snap,
        [(20, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_exclude=[16],
    )
    ids = {row["tmdb_id"] for row in with_exclude}
    assert 10 in ids


def test_genre_mask_before_topk_still_fills_limit():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=2,
        min_score=0,
        genre_exclude=[16],
    )
    assert len(out) == 2
    assert all(row["tmdb_id"] not in {30, 60} for row in out)


def test_media_type_plus_genre_mask():
    snap = _genre_snapshot()
    out = recommend(
        snap,
        [(10, "MOVIE")],
        "TV",
        limit=10,
        min_score=0,
        genre_include=[878],
    )
    assert out
    assert all(row["media_type"] == "TV" for row in out)
    assert {row["tmdb_id"] for row in out} == {40}


def test_genre_exclude_mask_inverted_would_fail():
    """Mutation check: exclude must drop hits, not keep only hits."""
    snap = _genre_snapshot()
    keep = build_genre_keep_mask(snap, [], [16])
    assert keep is not None
    assert not bool(keep[2])
    assert not bool(keep[5])
    assert bool(keep[1])
    out = recommend(
        snap,
        [(10, "MOVIE")],
        None,
        limit=10,
        min_score=0,
        genre_exclude=[16],
    )
    assert all(row["tmdb_id"] not in {30, 60} for row in out)
