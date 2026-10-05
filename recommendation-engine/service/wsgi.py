"""WSGI entrypoint: try to load the catalog once, then expose the Flask app."""

from __future__ import annotations

import logging

from app import create_app
from catalog_loader import EmptyCatalogError, load_snapshot
from catalog_store import CatalogStore
from config import REC_DATABASE_URL
from logging_setup import configure_logging

configure_logging()
logger = logging.getLogger(__name__)

store = CatalogStore()
try:
    store.reload(lambda: load_snapshot(REC_DATABASE_URL))
except EmptyCatalogError:
    logger.warning("catalog is empty, starting not ready until the next reload")
app = create_app(store)