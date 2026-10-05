"""Validate /rec/update JSON request bodies."""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Any

from config import (
    DEFAULT_LIMIT,
    MAX_EXCLUDE,
    MAX_GENRE_FILTER,
    MAX_LIMIT,
    MAX_LOVED,
    MEDIA_TYPES,
)


@dataclass(frozen=True)
class UpdateRequest:
    loved: list[tuple[int, str]]
    media_type: str | None
    limit: int
    min_score: float | None
    exclude: list[tuple[int, str]]
    genre_include: list[int]
    genre_exclude: list[int]


def _is_strict_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_finite_number(value: Any) -> bool:
    if not isinstance(value, (int, float)) or isinstance(value, bool):
        return False
    try:
        return math.isfinite(value)
    except OverflowError:
        return False


def _parse_media_key_item(item: Any, *, field: str) -> tuple[int, str]:
    """Validate one {tmdb_id, media_type} object for loved or exclude."""
    if not isinstance(item, dict):
        raise ValueError(f"{field} item must be an object")
    if "tmdb_id" not in item or "media_type" not in item:
        raise ValueError(f"{field} item missing fields")
    tmdb_id = item["tmdb_id"]
    media_type = item["media_type"]
    if not _is_strict_int(tmdb_id):
        raise ValueError("tmdb_id must be an int")
    if tmdb_id < 1:
        raise ValueError("tmdb_id out of range")
    if not isinstance(media_type, str) or media_type not in MEDIA_TYPES:
        raise ValueError(f"invalid {field} media_type")
    return (tmdb_id, media_type)


def _parse_genre_id_list(raw: Any, *, field: str) -> list[int]:
    """Validate optional genre id list. Empty or omitted means no filter."""
    if raw is None:
        return []
    if not isinstance(raw, list):
        raise ValueError(f"{field} must be a list")
    if len(raw) > MAX_GENRE_FILTER:
        raise ValueError(f"{field} length out of range")
    out: list[int] = []
    seen: set[int] = set()
    for item in raw:
        if not _is_strict_int(item) or item < 1:
            raise ValueError(f"{field} items must be positive ints")
        if item in seen:
            raise ValueError(f"{field} must not contain duplicates")
        seen.add(item)
        out.append(item)
    return out


def parse_update_request(body: Any) -> UpdateRequest:
    """Validate /rec/update JSON body."""
    if not isinstance(body, dict):
        raise ValueError("body must be a JSON object")

    if "loved" not in body:
        raise ValueError("loved is required")
    loved_raw = body["loved"]
    if not isinstance(loved_raw, list):
        raise ValueError("loved must be a list")
    if not 1 <= len(loved_raw) <= MAX_LOVED:
        raise ValueError("loved length out of range")

    loved: list[tuple[int, str]] = [
        _parse_media_key_item(item, field="loved") for item in loved_raw
    ]

    media_type: str | None
    if "media_type" not in body or body["media_type"] is None:
        media_type = None
    else:
        media_type = body["media_type"]
        if not isinstance(media_type, str) or media_type not in MEDIA_TYPES:
            raise ValueError("invalid media_type")

    if "limit" not in body or body["limit"] is None:
        limit = DEFAULT_LIMIT
    else:
        limit = body["limit"]
        if not _is_strict_int(limit):
            raise ValueError("limit must be an int")
        if not 1 <= limit <= MAX_LIMIT:
            raise ValueError("limit out of range")

    if "min_score" not in body or body["min_score"] is None:
        min_score = None
    else:
        raw_score = body["min_score"]
        if not _is_finite_number(raw_score) or not (0.0 <= float(raw_score) <= 1.0):
            raise ValueError("min_score must be a number between 0 and 1")
        min_score = float(raw_score)

    if "exclude" not in body or body["exclude"] is None:
        exclude: list[tuple[int, str]] = []
    else:
        exclude_raw = body["exclude"]
        if not isinstance(exclude_raw, list):
            raise ValueError("exclude must be a list")
        if len(exclude_raw) > MAX_EXCLUDE:
            raise ValueError("exclude length out of range")
        exclude = [
            _parse_media_key_item(item, field="exclude") for item in exclude_raw
        ]

    if "genre_include" not in body:
        genre_include: list[int] = []
    else:
        genre_include = _parse_genre_id_list(
            body["genre_include"], field="genre_include"
        )

    if "genre_exclude" not in body:
        genre_exclude: list[int] = []
    else:
        genre_exclude = _parse_genre_id_list(
            body["genre_exclude"], field="genre_exclude"
        )

    if set(genre_include) & set(genre_exclude):
        raise ValueError("genre_include and genre_exclude must not share an id")

    return UpdateRequest(
        loved=loved,
        media_type=media_type,
        limit=limit,
        min_score=min_score,
        exclude=exclude,
        genre_include=genre_include,
        genre_exclude=genre_exclude,
    )
