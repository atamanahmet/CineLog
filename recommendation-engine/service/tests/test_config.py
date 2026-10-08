"""Unit tests for token length and distinctness checks."""

import os

os.environ.setdefault("REC_DATABASE_URL", "postgres://test")
os.environ.setdefault("UPDATE_TOKEN", "test-update-token-xxxxxxxxxxxxxx")
os.environ.setdefault("RELOAD_TOKEN", "test-reload-token-xxxxxxxxxxxxxx")

import pytest

from config import (
    MAX_EXCLUDE,
    MAX_LIMIT,
    _ensure_default_within_max,
    _ensure_tokens_differ,
    _validate_token,
)


def test_validate_token_rejects_too_short():
    with pytest.raises(RuntimeError, match="UPDATE_TOKEN must be at least 32 characters"):
        _validate_token("UPDATE_TOKEN", "x" * 31)


def test_validate_token_accepts_exactly_32():
    _validate_token("UPDATE_TOKEN", "x" * 32)


def test_ensure_tokens_differ_rejects_equal():
    token = "y" * 32
    with pytest.raises(
        RuntimeError, match="UPDATE_TOKEN and RELOAD_TOKEN must not be equal"
    ):
        _ensure_tokens_differ(token, token)


def test_ensure_default_within_max_rejects_default_above_max():
    with pytest.raises(
        RuntimeError, match=r"DEFAULT_LIMIT \(60\) must be <= MAX_LIMIT \(50\)"
    ):
        _ensure_default_within_max(60, 50)


def test_ensure_default_within_max_accepts_equal():
    _ensure_default_within_max(50, 50)


def test_max_limit_default_is_100():
    assert MAX_LIMIT == 100


def test_max_exclude_default_is_10000():
    assert MAX_EXCLUDE == 10000


def test_update_rate_limit_default():
    from config import UPDATE_RATE_LIMIT

    assert UPDATE_RATE_LIMIT == "60 per minute"


def test_reload_rate_limit_default():
    from config import RELOAD_RATE_LIMIT

    assert RELOAD_RATE_LIMIT == "3 per hour"


def test_max_content_length_default():
    from config import MAX_CONTENT_LENGTH

    assert MAX_CONTENT_LENGTH == 262144
