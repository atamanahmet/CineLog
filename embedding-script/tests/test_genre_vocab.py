"""Tests for canonical genre id mapping."""

from __future__ import annotations

import pytest

from genre_vocab import GENRE_NAMES, MOVIE_GENRE_IDS, genre_name, to_canonical


def test_movie_ids_map_to_self_sorted_deduped():
    assert to_canonical("MOVIE", [35, 28, 28, 12]) == [12, 28, 35]


def test_tv_expand_and_map_ids():
    assert to_canonical("TV", [10759]) == [12, 28]
    assert to_canonical("TV", [10765]) == [14, 878]
    assert to_canonical("TV", [10768]) == [10752]
    assert to_canonical("TV", [10762]) == [10751]


def test_shared_and_tv_only_ids_stay():
    assert to_canonical("TV", [16, 10763, 10764, 10766, 10767]) == [
        16,
        10763,
        10764,
        10766,
        10767,
    ]


def test_unknown_id_kept():
    assert to_canonical("MOVIE", [99999, 28]) == [28, 99999]
    assert to_canonical("TV", [99999, 10759]) == [12, 28, 99999]


def test_unknown_media_type_raises():
    with pytest.raises(ValueError):
        to_canonical("BOOK", [28])


def test_every_canonical_name_id_has_a_name():
    tv_only = {10763, 10764, 10766, 10767}
    for gid in sorted(MOVIE_GENRE_IDS | tv_only):
        assert gid in GENRE_NAMES
        assert genre_name(gid) == GENRE_NAMES[gid]
        assert genre_name(gid)
