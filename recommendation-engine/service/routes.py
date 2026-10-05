"""HTTP routes for the recommendation engine."""

from __future__ import annotations

import logging

from flask import Blueprint, current_app, jsonify, request

from auth import requiring_token
from catalog_loader import EmptyCatalogError, load_snapshot
from catalog_store import ReloadInProgress
from config import REC_DATABASE_URL, RELOAD_TOKEN, UPDATE_TOKEN
from recommender import build_exclude_mask, recommend
from request_parsing import parse_update_request

logger = logging.getLogger(__name__)


def _deduct_unless_unauthorized(response) -> bool:
    """Count a hit only when the response is not an auth failure."""
    return response.status_code != 401


def create_blueprint(limiter, update_rate_limit: str, reload_rate_limit: str) -> Blueprint:
    bp = Blueprint("rec_engine", __name__)

    @bp.post("/rec/update")
    @limiter.limit(update_rate_limit, deduct_when=_deduct_unless_unauthorized)
    @requiring_token(lambda: UPDATE_TOKEN)
    def rec_update():
        try:
            req = parse_update_request(request.get_json(silent=True))
        except ValueError as exc:
            return jsonify({"error": str(exc)}), 400

        store = current_app.config["CATALOG_STORE"]
        snapshot = store.get()
        if snapshot is None:
            return jsonify({"error": "catalog unavailable"}), 503

        exclude_mask = build_exclude_mask(
            req.exclude,
            snapshot.index,
            int(snapshot.tmdb_ids.shape[0]),
        )
        return jsonify(
            recommend(
                snapshot,
                req.loved,
                req.media_type,
                req.limit,
                req.min_score,
                exclude_mask,
                req.genre_include,
                req.genre_exclude,
            )
        )

    @bp.post("/admin/reload-catalog")
    @limiter.limit(reload_rate_limit, deduct_when=_deduct_unless_unauthorized)
    @requiring_token(lambda: RELOAD_TOKEN)
    def reload_catalog():
        store = current_app.config["CATALOG_STORE"]
        try:
            snapshot = store.reload(lambda: load_snapshot(REC_DATABASE_URL))
        except ReloadInProgress:
            return jsonify({"error": "reload in progress"}), 409
        except EmptyCatalogError:
            logger.warning("catalog reload skipped, no embeddings stored")
            return jsonify({"error": "catalog is empty"}), 409
        except Exception:
            logger.exception("catalog reload failed")
            return jsonify({"error": "reload failed"}), 500
        return jsonify({"count": int(snapshot.tmdb_ids.shape[0])}), 200

    @bp.get("/health")
    def health():
        return jsonify({"status": "ok"}), 200

    @bp.get("/ready")
    def ready():
        store = current_app.config["CATALOG_STORE"]
        snapshot = store.get()
        if snapshot is None:
            return jsonify({"status": "loading"}), 503
        return jsonify(
            {"status": "ok", "rows": int(snapshot.tmdb_ids.shape[0])}
        ), 200

    return bp
