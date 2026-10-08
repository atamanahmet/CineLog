"""Ask the rec engine to reload its catalog."""

from __future__ import annotations

import logging

import requests

logger = logging.getLogger(__name__)


def request_reload(url: str, token: str, timeout_s: float) -> int | None:
    """POST to the reload endpoint. Returns the HTTP status, None if the request fails."""
    try:
        response = requests.post(
            url,
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout_s,
        )
    except requests.RequestException as exc:
        logger.warning("reload request failed: %s", type(exc).__name__)
        return None
    return int(response.status_code)
