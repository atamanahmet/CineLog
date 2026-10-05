"""Tests for backfill pure helpers and 429 retry."""

from __future__ import annotations

from unittest.mock import MagicMock

from backfill_genres import (
    detail_payload_to_row,
    fetch_detail,
    iter_batches,
)


def test_detail_payload_movie_maps_genres_and_vote_count():
    payload = {
        "genres": [{"id": 28, "name": "Action"}, {"id": 12, "name": "Adventure"}],
        "vote_count": 1500,
    }
    assert detail_payload_to_row("MOVIE", payload) == ([12, 28], 1500)


def test_detail_payload_tv_expands_genres():
    payload = {
        "genres": [{"id": 10759, "name": "Action & Adventure"}],
        "vote_count": 42,
    }
    assert detail_payload_to_row("TV", payload) == ([12, 28], 42)


def test_detail_payload_empty_genres_gives_empty_list():
    payload = {"genres": [], "vote_count": 0}
    assert detail_payload_to_row("MOVIE", payload) == ([], 0)


def test_iter_batches_splits_450_into_200_200_50():
    rows = list(range(450))
    batches = list(iter_batches(rows, 200))
    assert [len(b) for b in batches] == [200, 200, 50]
    assert batches[0][0] == 0
    assert batches[2][-1] == 449


def test_fetch_detail_retries_once_on_429():
    sleeps: list[float] = []
    responses = [
        MagicMock(status_code=429, headers={"Retry-After": "3"}),
        MagicMock(status_code=200),
    ]
    responses[1].json.return_value = {"genres": [], "vote_count": 1}

    class FakeSession:
        def __init__(self) -> None:
            self.calls = 0

        def get(self, url, timeout=None):
            self.calls += 1
            return responses[self.calls - 1]

    session = FakeSession()
    limiter = MagicMock()
    status, payload = fetch_detail(
        session,
        "MOVIE",
        10,
        limiter,
        sleep=sleeps.append,
    )
    assert status == "ok"
    assert payload == {"genres": [], "vote_count": 1}
    assert session.calls == 2
    assert sleeps == [3.0]
    assert limiter.wait.call_count == 2


def test_fetch_detail_429_twice_returns_failed():
    response = MagicMock(status_code=429, headers={"Retry-After": "1"})

    class FakeSession:
        def get(self, url, timeout=None):
            return response

    status, payload = fetch_detail(
        FakeSession(),
        "TV",
        20,
        MagicMock(),
        sleep=lambda _s: None,
    )
    assert status == "failed"
    assert payload is None
