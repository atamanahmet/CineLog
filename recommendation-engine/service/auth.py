"""Bearer token auth for protected recommendation engine routes."""

from __future__ import annotations

import hmac
import logging
from collections.abc import Callable
from functools import wraps
from typing import Any, TypeVar

from flask import jsonify, request

logger = logging.getLogger(__name__)

F = TypeVar("F", bound=Callable[..., Any])


def requiring_token(get_expected_token: Callable[[], str]) -> Callable[[F], F]:
    def decorator(fn: F) -> F:
        @wraps(fn)
        def wrapper(*args: Any, **kwargs: Any):
            header = request.headers.get("Authorization", "")
            expected = get_expected_token()
            parts = header.split(" ", 1)
            try:
                ok = (
                    len(parts) == 2
                    and parts[0] == "Bearer"
                    and bool(parts[1])
                    and hmac.compare_digest(parts[1], expected)
                )
            except (TypeError, ValueError):
                ok = False
            if not ok:
                route_name = request.endpoint or fn.__name__
                logger.warning("unauthorized request on %s", route_name)
                return jsonify({"error": "unauthorized"}), 401
            return fn(*args, **kwargs)

        return wrapper  # type: ignore[return-value]

    return decorator
