"""Flask application factory for the recommendation engine."""

from __future__ import annotations

from flask import Flask, jsonify
from flask_limiter import Limiter
from flask_limiter.errors import RateLimitExceeded
from werkzeug.exceptions import HTTPException

from catalog_store import CatalogStore
from config import MAX_CONTENT_LENGTH, RELOAD_RATE_LIMIT, UPDATE_RATE_LIMIT
from routes import create_blueprint


def _rate_limit_key() -> str:
    """Constant key for one shared bucket. The backend is the only caller."""
    return "global"


def create_app(
    store: CatalogStore,
    limiter_storage_uri: str = "memory://",
    update_rate_limit: str | None = None,
    reload_rate_limit: str | None = None,
) -> Flask:
    app = Flask(__name__)
    app.config["MAX_CONTENT_LENGTH"] = MAX_CONTENT_LENGTH
    app.config["CATALOG_STORE"] = store
    app.config["RATELIMIT_HEADERS_ENABLED"] = True

    limiter = Limiter(
        key_func=_rate_limit_key,
        app=app,
        storage_uri=limiter_storage_uri,
    )

    app.register_blueprint(
        create_blueprint(
            limiter,
            update_rate_limit or UPDATE_RATE_LIMIT,
            reload_rate_limit or RELOAD_RATE_LIMIT,
        )
    )

    @app.errorhandler(RateLimitExceeded)
    def handle_rate_limit(exc: RateLimitExceeded):
        response = jsonify({"error": "rate limit exceeded"})
        response.status_code = 429
        return response

    @app.errorhandler(HTTPException)
    def handle_http_exception(exc: HTTPException):
        return jsonify({"error": exc.name.lower()}), exc.code

    return app
