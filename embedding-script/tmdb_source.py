"""TMDB discover client with throttle and retries."""

from __future__ import annotations

import random
import time
from collections.abc import Callable, Iterator
from dataclasses import dataclass
from typing import Any

import requests

TMDB_API_HOST = "https://api.themoviedb.org"
MIN_REQUEST_INTERVAL_S = 0.125
REQUEST_TIMEOUT_S = 15
MAX_RETRIES = 5
BACKOFF_START_S = 2.0

MEDIA_ENDPOINTS: dict[str, tuple[str, str]] = {
    "MOVIE": ("/3/discover/movie", "title"),
    "TV": ("/3/discover/tv", "name"),
}
MEDIA_TYPES = tuple(MEDIA_ENDPOINTS)


class TmdbRequestError(Exception):
    """Raised when a TMDB request fails after all retries."""


@dataclass(frozen=True)
class CatalogItem:
    tmdb_id: int
    media_type: str
    title: str
    overview: str | None
    genre_ids: list[int] | None = None
    vote_count: int | None = None


class RequestThrottle:
    """Guarantee a minimum gap between successive requests."""

    def __init__(
        self,
        min_interval_s: float,
        clock: Callable[[], float],
        sleep: Callable[[float], None],
    ) -> None:
        self._min_interval_s = min_interval_s
        self._clock = clock
        self._sleep = sleep
        self._last_request_at: float | None = None

    def wait(self) -> None:
        now = self._clock()
        if self._last_request_at is not None:
            elapsed = now - self._last_request_at
            remaining = self._min_interval_s - elapsed
            if remaining > 0:
                self._sleep(remaining)
                now = self._clock()
        self._last_request_at = now


def create_session(api_key: str) -> requests.Session:
    """Build a session that sends the TMDB bearer token."""
    session = requests.Session()
    session.headers["Authorization"] = f"Bearer {api_key}"
    return session


def _backoff_delay(attempt: int, jitter: bool, rng: Callable[[], float]) -> float:
    delay = BACKOFF_START_S * (2**attempt)
    if jitter:
        delay += delay * rng()
    return delay


def request_json(
    session: requests.Session,
    path: str,
    params: dict[str, Any],
    throttle: RequestThrottle,
    *,
    sleep: Callable[[float], None] = time.sleep,
    rng: Callable[[], float] = random.random,
) -> dict[str, Any]:
    """GET JSON from TMDB with throttle and retries."""
    url = f"{TMDB_API_HOST}{path}"
    last_error: Exception | None = None

    for attempt in range(MAX_RETRIES + 1):
        throttle.wait()
        try:
            response = session.get(url, params=params, timeout=REQUEST_TIMEOUT_S)
        except (requests.ConnectionError, requests.Timeout) as exc:
            last_error = exc
            if attempt >= MAX_RETRIES:
                break
            sleep(_backoff_delay(attempt, jitter=True, rng=rng))
            continue

        status = response.status_code
        if status == 200:
            return response.json()

        if status == 429:
            last_error = TmdbRequestError(f"TMDB HTTP 429 at {path}")
            if attempt >= MAX_RETRIES:
                break
            retry_after = response.headers.get("Retry-After")
            if retry_after is not None and str(retry_after).strip().isdigit():
                sleep(float(retry_after))
            else:
                sleep(_backoff_delay(attempt, jitter=False, rng=rng))
            continue

        if 500 <= status <= 599:
            last_error = TmdbRequestError(f"TMDB HTTP {status} at {path}")
            if attempt >= MAX_RETRIES:
                break
            sleep(_backoff_delay(attempt, jitter=True, rng=rng))
            continue

        if 400 <= status <= 499:
            raise TmdbRequestError(f"TMDB HTTP {status} at {path}")

        last_error = TmdbRequestError(f"TMDB HTTP {status} at {path}")
        break

    if isinstance(last_error, TmdbRequestError):
        raise last_error
    raise TmdbRequestError(f"TMDB request failed at {path}") from last_error


def _optional_genre_ids(raw: dict[str, Any]) -> list[int] | None:
    """Read genre_ids from a discover item. Missing key is None."""
    if "genre_ids" not in raw:
        return None
    value = raw["genre_ids"]
    if value is None:
        return None
    return [int(g) for g in value]


def _optional_vote_count(raw: dict[str, Any]) -> int | None:
    """Read vote_count from a discover item. Missing key is None."""
    if "vote_count" not in raw:
        return None
    value = raw["vote_count"]
    if value is None:
        return None
    return int(value)


def _map_item(media_type: str, title_key: str, raw: dict[str, Any]) -> CatalogItem:
    """Map one discover dict to a CatalogItem. Movies use title, TV uses name."""
    return CatalogItem(
        tmdb_id=int(raw["id"]),
        media_type=media_type,
        title=str(raw.get(title_key) or ""),
        overview=raw.get("overview"),
        genre_ids=_optional_genre_ids(raw),
        vote_count=_optional_vote_count(raw),
    )


def iter_discover_pages(
    session: requests.Session,
    media_type: str,
    page_limit: int,
    throttle: RequestThrottle,
    *,
    sleep: Callable[[float], None] = time.sleep,
    rng: Callable[[], float] = random.random,
) -> Iterator[list[CatalogItem]]:
    """Yield discover result pages for one media type."""
    endpoint = MEDIA_ENDPOINTS.get(media_type)
    if endpoint is None:
        raise ValueError(f"unsupported media_type: {media_type}")
    path, title_key = endpoint

    limit = min(max(page_limit, 1), 500)
    for page in range(1, limit + 1):
        payload = request_json(
            session,
            path,
            {
                "sort_by": "popularity.desc",
                "include_adult": "false",
                "page": page,
            },
            throttle,
            sleep=sleep,
            rng=rng,
        )
        results = payload.get("results")
        if not isinstance(results, list):
            raise TmdbRequestError(f"TMDB invalid page body at {path}")
        yield [
            _map_item(media_type, title_key, item)
            for item in results
            if isinstance(item, dict)
        ]
