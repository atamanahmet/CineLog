"""Tests for genre-mix eval helpers."""

from __future__ import annotations

from genre_vocab import (
    GENRE_NAMES,
    MOVIE_GENRE_IDS,
    genre_name,
)
from eval_genres import (
    animation_stats,
    build_rec_update_payload,
    cached_title,
    collect_validation_problems,
    count_media_type_mismatch,
    format_result_line,
    genre_histogram,
    median_vote_count,
    normalize_title,
    titles_match,
)


def test_missing_catalog_seed_is_reported():
    sets = [
        {
            "name": "sci-fi movies",
            "media_type": "MOVIE",
            "seeds": [{"tmdb_id": 603, "title": "The Matrix"}],
        }
    ]
    problems = collect_validation_problems(sets, set(), {})
    assert len(problems) == 1
    assert "sci-fi movies" in problems[0]
    assert "603" in problems[0]
    assert "The Matrix" in problems[0]
    assert "not in catalog" in problems[0]


def test_title_mismatch_reported_punctuation_not_mismatch():
    sets = [
        {
            "name": "drama movies",
            "media_type": "MOVIE",
            "seeds": [
                {"tmdb_id": 424, "title": "Schindler's List"},
                {"tmdb_id": 13, "title": "Forrest Gump"},
            ],
        }
    ]
    catalog = {(424, "MOVIE"), (13, "MOVIE")}
    tmdb_titles = {
        (424, "MOVIE"): "Schindler\u2019s List",
        (13, "MOVIE"): "Wrong Gump",
    }
    problems = collect_validation_problems(sets, catalog, tmdb_titles)
    assert len(problems) == 1
    assert "13" in problems[0]
    assert "Forrest Gump" in problems[0]
    assert "Wrong Gump" in problems[0]
    assert titles_match("Schindler's List", "Schindler\u2019s List")
    assert normalize_title("Schindler's List") == normalize_title("Schindler\u2019s List")


def test_all_problems_collected_at_once():
    sets = [
        {
            "name": "mixed movies",
            "media_type": "MOVIE",
            "seeds": [
                {"tmdb_id": 1, "title": "A"},
                {"tmdb_id": 2, "title": "B"},
            ],
        }
    ]
    problems = collect_validation_problems(
        sets,
        {(2, "MOVIE")},
        {(2, "MOVIE"): "Not B"},
    )
    assert len(problems) == 2
    assert any("not in catalog" in p and "1" in p for p in problems)
    assert any("Not B" in p and "2" in p for p in problems)


def test_title_cache_fetches_repeated_id_once():
    calls: list[tuple[str, int]] = []

    def fetch(media_type: str, tmdb_id: int) -> str | None:
        calls.append((media_type, tmdb_id))
        return "Title"

    cache: dict[tuple[str, int], str | None] = {}
    assert cached_title(cache, "MOVIE", 10, fetch) == "Title"
    assert cached_title(cache, "MOVIE", 10, fetch) == "Title"
    assert calls == [("MOVIE", 10)]


def test_failed_fetch_gives_question_mark():
    cache: dict[tuple[str, int], str | None] = {}
    assert cached_title(cache, "TV", 99, lambda _m, _i: None) == "?"


def test_animation_stats_with_and_without_and_null():
    count, pct = animation_stats([[16, 28], [18], None, []])
    assert count == 1
    assert abs(pct - 25.0) < 1e-9
    count0, pct0 = animation_stats([[18], [28]])
    assert count0 == 0
    assert pct0 == 0.0


def test_genre_histogram_order_and_limit():
    rows = [
        ["Drama", "Action"],
        ["Drama", "Comedy"],
        ["Drama"],
        ["Action"],
        ["Zebra"],
    ]
    # pad with unique names so limit 10 is testable with ties
    for i in range(6):
        rows.append([f"Extra{i}"])
    top = genre_histogram(rows, limit=3)
    assert top == [("Drama", 3), ("Action", 2), ("Comedy", 1)]


def test_median_odd_even_and_nulls_ignored():
    assert median_vote_count([1, 3, 2]) == 2
    assert median_vote_count([1, 2, 3, 4]) == 2
    assert median_vote_count([10, None, 30, None]) == 20
    assert median_vote_count([None, None]) is None


def test_format_result_line_null_genres():
    line = format_result_line(1, "Film", "MOVIE", [], None, 0.91234)
    assert line.startswith("1\tFilm\tMOVIE\t")
    assert "0.9123" in line
    parts = line.split("\t")
    assert parts[3] == ""


def test_every_canonical_name_id_has_a_name():
    tv_only = {10763, 10764, 10766, 10767}
    for gid in sorted(MOVIE_GENRE_IDS | tv_only):
        assert gid in GENRE_NAMES
        assert genre_name(gid)
        assert genre_name(gid) == GENRE_NAMES[gid]


def test_build_rec_update_payload_includes_media_type():
    payload = build_rec_update_payload(
        [(603, "MOVIE"), (78, "MOVIE")],
        "MOVIE",
        50,
    )
    assert payload["media_type"] == "MOVIE"
    assert payload["limit"] == 50
    assert payload["loved"] == [
        {"tmdb_id": 603, "media_type": "MOVIE"},
        {"tmdb_id": 78, "media_type": "MOVIE"},
    ]
    assert set(payload.keys()) == {"loved", "media_type", "limit"}


def test_count_media_type_mismatch_zero_some_all():
    assert count_media_type_mismatch(["MOVIE", "MOVIE"], "MOVIE") == 0
    assert count_media_type_mismatch(["MOVIE", "TV", "MOVIE"], "MOVIE") == 1
    assert count_media_type_mismatch(["TV", "TV"], "MOVIE") == 2
