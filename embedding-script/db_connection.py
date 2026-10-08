"""Postgres connections that wait for a sleeping Neon compute to wake up."""

from __future__ import annotations

import logging
import time
from collections.abc import Callable, Iterator
from contextlib import contextmanager
from dataclasses import dataclass

import psycopg2
from psycopg2.extensions import connection as PgConnection
from psycopg2.extensions import parse_dsn

BACKOFF_START_S = 1.0
BACKOFF_MAX_S = 15.0

logger = logging.getLogger(__name__)


class DatabaseUnavailableError(Exception):
    """Raised when the database is still not reachable after all attempts."""


@dataclass(frozen=True)
class RetryPolicy:
    attempts: int
    connect_timeout_s: int


def parse_target(url: str) -> tuple[str, str]:
    """Return host and database name of a connection URL. Never the password."""
    try:
        parts = parse_dsn(url)
    except psycopg2.ProgrammingError:
        raise ValueError("not a valid connection URL") from None
    return parts.get("host", ""), parts.get("dbname", "")


def _backoff_delay_s(attempt: int) -> float:
    """Wait time after a failed attempt. Doubles each time up to a cap."""
    return min(BACKOFF_START_S * (2**attempt), BACKOFF_MAX_S)


def connect_with_retry(
    url: str,
    policy: RetryPolicy,
    *,
    connect: Callable[..., PgConnection] = psycopg2.connect,
    sleep: Callable[[float], None] = time.sleep,
) -> PgConnection:
    """Connect to Postgres and retry while the server is not ready."""
    last_error: psycopg2.OperationalError | None = None
    for attempt in range(policy.attempts):
        try:
            return connect(url, connect_timeout=policy.connect_timeout_s)
        except psycopg2.OperationalError as exc:
            last_error = exc
            if attempt + 1 == policy.attempts:
                break
            delay = _backoff_delay_s(attempt)
            logger.warning(
                "database not ready attempt=%s/%s retry_in_s=%.0f reason=%s",
                attempt + 1,
                policy.attempts,
                delay,
                str(exc).strip().splitlines()[0] if str(exc).strip() else "unknown",
            )
            sleep(delay)
    raise DatabaseUnavailableError(
        f"database not reachable after {policy.attempts} attempts"
    ) from last_error


@contextmanager
def open_connection(url: str, policy: RetryPolicy) -> Iterator[PgConnection]:
    """Yield a connected session and always close it."""
    conn = connect_with_retry(url, policy)
    try:
        yield conn
    finally:
        conn.close()
