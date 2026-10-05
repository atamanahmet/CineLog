"""Tests for embedding script helpers and TMDB source behavior."""

from __future__ import annotations

import logging
from unittest.mock import MagicMock

import numpy as np
import pytest

import embed
from embed import (
    build_text,
    embedding_row_fields,
    ping_reload,
    skip_reason,
    to_float16_bytes,
)
from tmdb_source import (
    BACKOFF_START_S,
    MAX_RETRIES,
    MIN_REQUEST_INTERVAL_S,
    CatalogItem,
    RequestThrottle,
    TmdbRequestError,
    _map_movie,
    _map_tv,
    iter_discover_pages,
    request_json,
)


def _patch_main_io(monkeypatch, *, page_map, flush_side_effect=None):
    """Stub DB/model/TMDB I/O so main() can run without network."""
    monkeypatch.setattr(
        embed,
        "require_env",
        lambda: {
            "TMDB_API_KEY": "k",
            "REC_WRITE_DATABASE_URL": "postgres://test",
            "RELOAD_URL": "http://reload.test/admin/reload-catalog",
            "RELOAD_TOKEN": "t" * 32,
        },
    )
    conn = MagicMock()
    monkeypatch.setattr(embed.psycopg2, "connect", lambda _url: conn)
    monkeypatch.setattr(embed, "read_stored_keys", lambda _conn, _model: set())
    monkeypatch.setattr(embed, "load_model", lambda: object())
    monkeypatch.setattr(embed, "create_session", lambda _key: object())

    def fake_pages(_session, media_type, _pages, _throttle, **_kwargs):
        yield from page_map.get(media_type, [])

    monkeypatch.setattr(embed, "iter_discover_pages", fake_pages)

    pings: list[tuple[str, str]] = []

    def fake_ping(url: str, token: str):
        pings.append((url, token))
        return 200

    monkeypatch.setattr(embed, "ping_reload", fake_ping)

    flushes: list[list[CatalogItem]] = []

    def fake_flush(conn, model, chunk, skipped_stored, skipped_empty):
        flushes.append(list(chunk))
        if flush_side_effect is not None:
            flush_side_effect()
        return len(chunk)

    monkeypatch.setattr(embed, "flush_chunk", fake_flush)
    return pings, flushes


class FakeClock:
    def __init__(self, start: float = 1000.0) -> None:
        self.now = start

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


class FakeSleep:
    def __init__(self, clock: FakeClock) -> None:
        self.clock = clock
        self.calls: list[float] = []

    def __call__(self, seconds: float) -> None:
        self.calls.append(seconds)
        self.clock.advance(seconds)


class FakeResponse:
    def __init__(
        self,
        status_code: int,
        payload: dict | None = None,
        headers: dict | None = None,
    ) -> None:
        self.status_code = status_code
        self._payload = payload or {}
        self.headers = headers or {}

    def json(self) -> dict:
        return self._payload


class FakeSession:
    def __init__(self, responses: list[FakeResponse | Exception]) -> None:
        self._responses = list(responses)
        self.calls: list[tuple[str, dict, float | None]] = []

    def get(self, url: str, params=None, timeout=None):
        self.calls.append((url, dict(params or {}), timeout))
        if not self._responses:
            raise AssertionError("unexpected extra request")
        item = self._responses.pop(0)
        if isinstance(item, Exception):
            raise item
        return item


def test_build_text_with_overview():
    assert build_text("Title", "Plot") == "Title. Plot"


def test_float16_bytes_length_768_for_384_dim():
    vector = np.zeros(384, dtype=np.float32)
    raw = to_float16_bytes(vector)
    assert len(raw) == 768


def test_skip_already_stored():
    item = CatalogItem(1, "MOVIE", "A", "plot")
    assert skip_reason(item, {(1, "MOVIE")}, set()) == "stored"


def test_skip_empty_overview():
    item = CatalogItem(1, "MOVIE", "A", "   ")
    assert skip_reason(item, set(), set()) == "empty"
    assert skip_reason(CatalogItem(1, "MOVIE", "A", None), set(), set()) == "empty"


def test_skip_duplicate_in_run():
    item = CatalogItem(1, "MOVIE", "A", "plot")
    assert skip_reason(item, set(), {(1, "MOVIE")}) == "duplicate"


def test_keep_new_item():
    item = CatalogItem(1, "MOVIE", "A", "plot")
    assert skip_reason(item, set(), set()) is None


def test_throttle_spaces_requests():
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(MIN_REQUEST_INTERVAL_S, clock, sleep)
    throttle.wait()
    assert sleep.calls == []
    clock.advance(0.05)
    throttle.wait()
    assert len(sleep.calls) == 1
    assert abs(sleep.calls[0] - (MIN_REQUEST_INTERVAL_S - 0.05)) < 1e-9


def test_retry_after_honored_on_429():
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(0.0, clock, sleep)
    session = FakeSession(
        [
            FakeResponse(429, headers={"Retry-After": "7"}),
            FakeResponse(200, {"results": []}),
        ]
    )
    payload = request_json(
        session,
        "/3/discover/movie",
        {"page": 1},
        throttle,
        sleep=sleep,
        rng=lambda: 0.0,
    )
    assert payload == {"results": []}
    assert 7.0 in sleep.calls


def test_backoff_on_500_then_success():
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(0.0, clock, sleep)
    session = FakeSession(
        [
            FakeResponse(500),
            FakeResponse(200, {"ok": True}),
        ]
    )
    payload = request_json(
        session,
        "/3/discover/movie",
        {"page": 1},
        throttle,
        sleep=sleep,
        rng=lambda: 0.0,
    )
    assert payload == {"ok": True}
    assert sleep.calls[0] == BACKOFF_START_S


def test_exception_after_five_failed_retries():
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(0.0, clock, sleep)
    session = FakeSession([FakeResponse(500) for _ in range(MAX_RETRIES + 1)])
    with pytest.raises(TmdbRequestError, match="TMDB HTTP 500 at /3/discover/movie"):
        request_json(
            session,
            "/3/discover/movie",
            {"page": 1},
            throttle,
            sleep=sleep,
            rng=lambda: 0.0,
        )
    assert len(session.calls) == MAX_RETRIES + 1


def test_4xx_message_has_no_secret():
    secret = "super-secret-tmdb-key-value"
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(0.0, clock, sleep)
    session = FakeSession([FakeResponse(401)])
    with pytest.raises(TmdbRequestError) as exc_info:
        request_json(
            session,
            "/3/discover/movie",
            {"page": 1},
            throttle,
            sleep=sleep,
            rng=lambda: 0.0,
        )
    message = str(exc_info.value)
    assert secret not in message
    assert "Authorization" not in message
    assert message == "TMDB HTTP 401 at /3/discover/movie"


def test_map_movie_carries_genre_ids_and_vote_count():
    item = _map_movie(
        {
            "id": 10,
            "title": "Film",
            "overview": "A plot",
            "genre_ids": [28, 12],
            "vote_count": 99,
        }
    )
    assert item.genre_ids == [28, 12]
    assert item.vote_count == 99


def test_map_tv_carries_genre_ids_and_vote_count():
    item = _map_tv(
        {
            "id": 20,
            "name": "Show",
            "overview": "TV plot",
            "genre_ids": [10759, 18],
            "vote_count": 7,
        }
    )
    assert item.genre_ids == [10759, 18]
    assert item.vote_count == 7


def test_map_missing_keys_give_none():
    movie = _map_movie({"id": 1, "title": "A", "overview": "p"})
    tv = _map_tv({"id": 2, "name": "B", "overview": "p"})
    assert movie.genre_ids is None
    assert movie.vote_count is None
    assert tv.genre_ids is None
    assert tv.vote_count is None


def test_map_empty_genre_list_stays_empty():
    movie = _map_movie(
        {"id": 1, "title": "A", "overview": "p", "genre_ids": [], "vote_count": 0}
    )
    assert movie.genre_ids == []
    assert movie.vote_count == 0


def test_embedding_row_fields_passes_canonical_ids():
    item = CatalogItem(
        1,
        "TV",
        "Show",
        "plot",
        genre_ids=[10765, 18, 18],
        vote_count=33,
    )
    assert embedding_row_fields(item) == ([14, 18, 878], 33)


def test_embedding_row_fields_none_genre_ids_stay_none():
    item = CatalogItem(1, "MOVIE", "A", "plot", genre_ids=None, vote_count=5)
    assert embedding_row_fields(item) == (None, 5)


def test_map_movie_and_tv_pages():
    clock = FakeClock()
    sleep = FakeSleep(clock)
    throttle = RequestThrottle(0.0, clock, sleep)
    movie_session = FakeSession(
        [
            FakeResponse(
                200,
                {
                    "results": [
                        {"id": 10, "title": "Film", "overview": "A plot"},
                    ]
                },
            )
        ]
    )
    movie_pages = list(
        iter_discover_pages(
            movie_session,
            "MOVIE",
            page_limit=1,
            throttle=throttle,
            sleep=sleep,
            rng=lambda: 0.0,
        )
    )
    assert movie_pages == [
        [CatalogItem(10, "MOVIE", "Film", "A plot")]
    ]

    tv_session = FakeSession(
        [
            FakeResponse(
                200,
                {
                    "results": [
                        {"id": 20, "name": "Show", "overview": "TV plot"},
                    ]
                },
            )
        ]
    )
    tv_pages = list(
        iter_discover_pages(
            tv_session,
            "TV",
            page_limit=1,
            throttle=throttle,
            sleep=sleep,
            rng=lambda: 0.0,
        )
    )
    assert tv_pages == [[CatalogItem(20, "TV", "Show", "TV plot")]]
    assert "language" not in movie_session.calls[0][1]
    assert movie_session.calls[0][1]["sort_by"] == "popularity.desc"
    assert movie_session.calls[0][1]["include_adult"] == "false"


def test_ping_reload_logs_exception_class(monkeypatch, caplog):
    def boom(_req, timeout=None):
        raise TimeoutError("slow")

    monkeypatch.setattr(embed.urllib.request, "urlopen", boom)
    with caplog.at_level(logging.WARNING):
        assert ping_reload("http://example.test/reload", "token") is None
    assert "TimeoutError" in caplog.text
    assert "token" not in caplog.text
    assert "example.test" not in caplog.text


def test_main_pings_when_nothing_written(monkeypatch):
    item = CatalogItem(1, "MOVIE", "A", "plot")
    monkeypatch.setattr(
        embed,
        "require_env",
        lambda: {
            "TMDB_API_KEY": "k",
            "REC_WRITE_DATABASE_URL": "postgres://test",
            "RELOAD_URL": "http://reload.test/admin/reload-catalog",
            "RELOAD_TOKEN": "t" * 32,
        },
    )
    monkeypatch.setattr(embed.psycopg2, "connect", lambda _url: MagicMock())
    monkeypatch.setattr(
        embed, "read_stored_keys", lambda _conn, _model: {(1, "MOVIE"), (2, "TV")}
    )
    monkeypatch.setattr(embed, "load_model", lambda: object())
    monkeypatch.setattr(embed, "create_session", lambda _key: object())

    def fake_pages(_session, media_type, _pages, _throttle, **_kwargs):
        if media_type == "MOVIE":
            yield [item]
        else:
            yield [CatalogItem(2, "TV", "B", "plot")]

    monkeypatch.setattr(embed, "iter_discover_pages", fake_pages)
    pings: list[int] = []
    monkeypatch.setattr(
        embed, "ping_reload", lambda _url, _token: pings.append(1) or 200
    )
    monkeypatch.setattr(
        embed, "flush_chunk", lambda *_a, **_k: (_ for _ in ()).throw(AssertionError())
    )

    embed.main(["--pages", "1"])
    assert pings == [1]


def test_main_pings_after_tmdb_error_following_flush(monkeypatch):
    pings, flushes = _patch_main_io(monkeypatch, page_map={})

    def fake_pages(_session, media_type, _pages, _throttle, **_kwargs):
        if media_type == "MOVIE":
            yield [CatalogItem(10, "MOVIE", "Film", "plot")]
            return
        raise TmdbRequestError("TMDB HTTP 500 at /3/discover/tv")

    monkeypatch.setattr(embed, "iter_discover_pages", fake_pages)

    with pytest.raises(SystemExit) as exc_info:
        embed.main(["--pages", "1"])
    assert exc_info.value.code == 1
    assert flushes == [[CatalogItem(10, "MOVIE", "Film", "plot")]]
    assert len(pings) == 1
