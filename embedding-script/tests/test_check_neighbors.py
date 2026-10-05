"""Unit tests for check_neighbors.compare_results only."""

from __future__ import annotations

from check_neighbors import compare_results


def test_matching_data_passes():
    query = (1, "MOVIE")
    engine = [
        {"tmdb_id": 2, "media_type": "MOVIE", "score": 0.9},
        {"tmdb_id": 3, "media_type": "TV", "score": 0.8},
    ]
    local = {
        (2, "MOVIE"): 0.9,
        (3, "TV"): 0.8,
        (4, "MOVIE"): 0.1,
    }
    assert compare_results(engine, local, query) == []


def test_score_mismatch_fails():
    query = (1, "MOVIE")
    engine = [{"tmdb_id": 2, "media_type": "MOVIE", "score": 0.9}]
    local = {(2, "MOVIE"): 0.5}
    failures = compare_results(engine, local, query)
    assert any("score mismatch" in message for message in failures)


def test_better_missing_candidate_fails():
    query = (1, "MOVIE")
    engine = [
        {"tmdb_id": 2, "media_type": "MOVIE", "score": 0.7},
        {"tmdb_id": 3, "media_type": "MOVIE", "score": 0.6},
    ]
    local = {
        (2, "MOVIE"): 0.7,
        (3, "MOVIE"): 0.6,
        (4, "TV"): 0.85,
    }
    failures = compare_results(engine, local, query)
    assert any("missing candidate" in message for message in failures)


def test_query_key_in_results_fails():
    query = (1, "MOVIE")
    engine = [
        {"tmdb_id": 1, "media_type": "MOVIE", "score": 1.0},
        {"tmdb_id": 2, "media_type": "MOVIE", "score": 0.9},
    ]
    local = {
        (2, "MOVIE"): 0.9,
        (3, "MOVIE"): 0.1,
    }
    failures = compare_results(engine, local, query)
    assert any("appears in results" in message for message in failures)


def test_empty_engine_results_skips_missing_candidate_check():
    query = (1, "MOVIE")
    local = {(2, "MOVIE"): 0.99}
    assert compare_results([], local, query) == []


def test_unsorted_results_fail():
    query = (1, "MOVIE")
    engine = [
        {"tmdb_id": 2, "media_type": "MOVIE", "score": 0.5},
        {"tmdb_id": 3, "media_type": "MOVIE", "score": 0.9},
    ]
    local = {(2, "MOVIE"): 0.5, (3, "MOVIE"): 0.9}
    failures = compare_results(engine, local, query)
    assert any("not sorted" in message for message in failures)
