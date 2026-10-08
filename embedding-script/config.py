"""Settings for the embedding script, read from env files and the environment."""

from __future__ import annotations

import os
from collections.abc import Callable
from dataclasses import dataclass
from typing import TypeVar
from urllib.parse import urlparse

from dotenv import dotenv_values, find_dotenv, load_dotenv

from db_connection import RetryPolicy, parse_target

MODE_ENV = "CINELOG_MODE"
DEV_MODE = "dev"
NEON_MODE = "neon"
MODES = (DEV_MODE, NEON_MODE)
LOCAL_HOSTS = frozenset({"localhost", "127.0.0.1", "::1"})
RELOAD_ENV_NAMES = ("REC_ENGINE_BASE_URL", "RELOAD_TOKEN")
RELOAD_PATH = "/reload"

DEFAULT_RELOAD_TIMEOUT_S = 120.0
DEFAULT_DB_CONNECT_TIMEOUT_S = 10
DEFAULT_DB_CONNECT_RETRIES = 5

Number = TypeVar("Number", int, float)


@dataclass(frozen=True)
class Settings:
    mode: str
    tmdb_api_key: str
    database_url: str
    reload_url: str | None
    reload_token: str | None
    reload_timeout_s: float
    db_retry: RetryPolicy


def _optional(name: str) -> str | None:
    """Read a variable. Missing or blank is None."""
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return None
    return raw.strip()


def _required(name: str) -> str:
    """Read a variable or stop the run."""
    value = _optional(name)
    if value is None:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def _number(name: str, default: Number, cast: Callable[[str], Number]) -> Number:
    """Read a numeric variable, or return the default when it is not set."""
    raw = _optional(name)
    if raw is None:
        return default
    try:
        return cast(raw)
    except ValueError:
        raise SystemExit(f"{name} must be a number, got: {raw}") from None


def read_mode() -> str:
    """Read the run mode from CINELOG_MODE. No value means dev."""
    mode = _optional(MODE_ENV) or DEV_MODE
    if mode not in MODES:
        raise SystemExit(f"{MODE_ENV} must be one of {MODES}, got: {mode}")
    return mode


def _drop_inherited_reload_target(neon_values: dict[str, str | None]) -> None:
    """Forget the reload target from .env unless .env.neon sets its own."""
    for name in RELOAD_ENV_NAMES:
        if name not in neon_values:
            os.environ.pop(name, None)


def load_env_files(mode: str) -> None:
    """Load .env, then .env.neon on top in neon mode."""
    load_dotenv(find_dotenv(".env", usecwd=True))
    if mode != NEON_MODE:
        return
    neon_path = find_dotenv(".env.neon", usecwd=True)
    if not neon_path:
        raise SystemExit(".env.neon not found, copy .env.neon.example to .env.neon")
    load_dotenv(neon_path, override=True)
    _drop_inherited_reload_target(dotenv_values(neon_path))


def _database_url(mode: str) -> str:
    """Read the write URL. Neon mode refuses a local host."""
    url = _required("REC_WRITE_DATABASE_URL")
    try:
        host, _ = parse_target(url)
    except ValueError as exc:
        raise SystemExit(f"REC_WRITE_DATABASE_URL is invalid: {exc}") from None
    if mode == NEON_MODE and (not host or host in LOCAL_HOSTS):
        raise SystemExit(
            "neon mode, but REC_WRITE_DATABASE_URL points to a local host. "
            "Set it in .env.neon"
        )
    return url


def _reload_target() -> tuple[str | None, str | None]:
    """Build the reload URL from the engine base URL. Base URL and token, both or none."""
    base_url = _optional("REC_ENGINE_BASE_URL")
    token = _optional("RELOAD_TOKEN")
    if (base_url is None) != (token is None):
        raise SystemExit("REC_ENGINE_BASE_URL and RELOAD_TOKEN must be set together")
    if base_url is None:
        return None, None
    if urlparse(base_url).scheme not in ("http", "https"):
        raise SystemExit("REC_ENGINE_BASE_URL must start with http:// or https://")
    return base_url.rstrip("/") + RELOAD_PATH, token


def _retry_policy() -> RetryPolicy:
    """Build the connect retry policy. Same variable names as the rec engine."""
    timeout_s = _number("DB_CONNECT_TIMEOUT_SECONDS", DEFAULT_DB_CONNECT_TIMEOUT_S, int)
    retries = _number("DB_CONNECT_RETRIES", DEFAULT_DB_CONNECT_RETRIES, int)
    if timeout_s < 1:
        raise SystemExit("DB_CONNECT_TIMEOUT_SECONDS must be >= 1")
    if retries < 0:
        raise SystemExit("DB_CONNECT_RETRIES must be >= 0")
    return RetryPolicy(attempts=retries + 1, connect_timeout_s=timeout_s)


def load_settings() -> Settings:
    """Load env files for the active mode and build validated settings."""
    mode = read_mode()
    load_env_files(mode)
    reload_url, reload_token = _reload_target()
    return Settings(
        mode=mode,
        tmdb_api_key=_required("TMDB_API_KEY"),
        database_url=_database_url(mode),
        reload_url=reload_url,
        reload_token=reload_token,
        reload_timeout_s=_number("RELOAD_HTTP_TIMEOUT_S", DEFAULT_RELOAD_TIMEOUT_S, float),
        db_retry=_retry_policy(),
    )