"""Thread-safe holder for the current catalog snapshot."""

from __future__ import annotations

import threading
from collections.abc import Callable

from catalog import CatalogSnapshot


class ReloadInProgress(Exception):
    """Another catalog reload is already running."""


class CatalogStore:
    def __init__(self) -> None:
        self._snapshot: CatalogSnapshot | None = None
        self._lock = threading.Lock()

    def get(self) -> CatalogSnapshot | None:
        return self._snapshot

    def reload(self, loader: Callable[[], CatalogSnapshot]) -> CatalogSnapshot:
        if not self._lock.acquire(blocking=False):
            raise ReloadInProgress("catalog reload already in progress")
        try:
            snapshot = loader()
            self._snapshot = snapshot
            return snapshot
        finally:
            self._lock.release()
