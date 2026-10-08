"""Load recommendation engine settings from the environment once."""

from __future__ import annotations

import os

_MIN_TOKEN_LENGTH = 32


def _require(name: str) -> str:
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        raise RuntimeError(f"Missing required environment variable: {name}")
    return raw.strip()


def _validate_token(name: str, value: str) -> None:
    """Reject a token shorter than the minimum length."""
    if len(value) < _MIN_TOKEN_LENGTH:
        raise RuntimeError(
            f"{name} must be at least {_MIN_TOKEN_LENGTH} characters"
        )


def _ensure_tokens_differ(update_token: str, reload_token: str) -> None:
    """Reject matching update and reload tokens."""
    if update_token == reload_token:
        raise RuntimeError("UPDATE_TOKEN and RELOAD_TOKEN must not be equal")


def _ensure_default_within_max(default_limit: int, max_limit: int) -> None:
    """Reject a default limit above the hard max."""
    if default_limit > max_limit:
        raise RuntimeError(
            f"DEFAULT_LIMIT ({default_limit}) must be <= MAX_LIMIT ({max_limit})"
        )


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return default
    return int(raw.strip())


def _env_float(name: str, default: float) -> float:
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return default
    return float(raw.strip())


def _env_str(name: str, default: str) -> str:
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return default
    return raw.strip()


REC_DATABASE_URL = _require("REC_DATABASE_URL")
UPDATE_TOKEN = _require("UPDATE_TOKEN")
_validate_token("UPDATE_TOKEN", UPDATE_TOKEN)
RELOAD_TOKEN = _require("RELOAD_TOKEN")
_validate_token("RELOAD_TOKEN", RELOAD_TOKEN)
_ensure_tokens_differ(UPDATE_TOKEN, RELOAD_TOKEN)

SIMILARITY_THRESHOLD = _env_float("SIMILARITY_THRESHOLD", 0.3)
DEFAULT_LIMIT = _env_int("DEFAULT_LIMIT", 20)
MAX_LIMIT = _env_int("MAX_LIMIT", 100)
_ensure_default_within_max(DEFAULT_LIMIT, MAX_LIMIT)
MAX_LOVED = _env_int("MAX_LOVED", 500)
MAX_EXCLUDE = _env_int("REC_MAX_EXCLUDE", 10000)
MAX_GENRE_FILTER = 25
UPDATE_RATE_LIMIT = _env_str("UPDATE_RATE_LIMIT", "60 per minute")
RELOAD_RATE_LIMIT = _env_str("RELOAD_RATE_LIMIT", "3 per hour")
DB_CONNECT_TIMEOUT_SECONDS = _env_int("DB_CONNECT_TIMEOUT_SECONDS", 10)
DB_CONNECT_RETRIES = _env_int("DB_CONNECT_RETRIES", 5)
MAX_CONTENT_LENGTH = _env_int("MAX_CONTENT_LENGTH", 262144)

EMBEDDING_MODEL_ID = 1
EMBEDDING_DIM = 384
VECTOR_BYTES = EMBEDDING_DIM * 2
NORM_TOLERANCE = 0.01

MEDIA_TYPES = ("MOVIE", "TV")
