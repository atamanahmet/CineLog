"""Embed TMDB discover titles into catalog_embedding, then ping reload."""

from __future__ import annotations

import argparse
import logging
import sys

from config import Settings, load_settings
from db_connection import DatabaseUnavailableError, parse_target
from pipeline import run_embedding
from reload_client import request_reload

logger = logging.getLogger(__name__)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Parse CLI args. --pages is the discover page count per media type."""
    parser = argparse.ArgumentParser(description="Embed TMDB discover pages")
    parser.add_argument(
        "--pages",
        type=int,
        default=500,
        help="Discover pages per media type (1-500)",
    )
    args = parser.parse_args(argv)
    if not 1 <= args.pages <= 500:
        parser.error("--pages must be between 1 and 500")
    return args


def reload_if_configured(settings: Settings) -> None:
    """Ask the rec engine to reload. Missing target or failure is only a warning."""
    if settings.reload_url is None or settings.reload_token is None:
        logger.warning("reload skipped, no rec engine configured")
        return
    status = request_reload(
        settings.reload_url, settings.reload_token, settings.reload_timeout_s
    )
    if status is not None and 200 <= status < 300:
        logger.info("reload result=ok status=%s", status)
    else:
        logger.warning("reload result=failed status=%s", status)


def main(argv: list[str] | None = None) -> int:
    """Run the embedding job. Returns the process exit code."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    args = parse_args(argv)
    settings = load_settings()
    host, database = parse_target(settings.database_url)
    logger.info("run mode=%s target_host=%s target_db=%s", settings.mode, host, database)

    try:
        result = run_embedding(settings, args.pages)
    except DatabaseUnavailableError as exc:
        logger.error("%s", exc)
        return 1

    if result.written == 0:
        logger.info("nothing to embed")
    reload_if_configured(settings)
    logger.info("done total_written=%s tmdb_failed=%s", result.written, result.tmdb_failed)
    return 1 if result.tmdb_failed else 0


if __name__ == "__main__":
    sys.exit(main())
