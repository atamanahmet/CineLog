import os

os.environ.setdefault("REC_DATABASE_URL", "postgres://test")
os.environ.setdefault("UPDATE_TOKEN", "test-update-token-xxxxxxxxxxxxxx")
os.environ.setdefault("RELOAD_TOKEN", "test-reload-token-xxxxxxxxxxxxxx")
os.environ.setdefault("SIMILARITY_THRESHOLD", "0.3")
os.environ.setdefault("MAX_LIMIT", "100")
os.environ.setdefault("DEFAULT_LIMIT", "20")
os.environ.setdefault("MAX_LOVED", "500")

import numpy as np
import pytest

from app import create_app
from catalog import CatalogSnapshot, empty_genre_membership
from catalog_store import CatalogStore
from config import DEFAULT_LIMIT, MAX_CONTENT_LENGTH, RELOAD_TOKEN, UPDATE_TOKEN
from request_parsing import UpdateRequest, parse_update_request


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _l2(rows: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(rows, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    return (rows / norms).astype(np.float32)


def _small_snapshot() -> CatalogSnapshot:
    raw = np.array(
        [
            [1.0, 0.0, 0.0, 0.0],
            [0.8, 0.6, 0.0, 0.0],
            [0.0, 1.0, 0.0, 0.0],
            [0.6, 0.8, 0.0, 0.0],
        ],
        dtype=np.float32,
    )
    matrix = _l2(raw)
    tmdb_ids = np.array([10, 20, 30, 40], dtype=np.int64)
    media_types = np.array(["MOVIE", "MOVIE", "MOVIE", "TV"], dtype=object)
    index = {
        (10, "MOVIE"): 0,
        (20, "MOVIE"): 1,
        (30, "MOVIE"): 2,
        (40, "TV"): 3,
    }
    genre_matrix, genre_col_index = empty_genre_membership(4)
    return CatalogSnapshot(
        tmdb_ids=tmdb_ids,
        media_types=media_types,
        matrix=matrix,
        index=index,
        genre_matrix=genre_matrix,
        genre_col_index=genre_col_index,
    )


def _store_with_snapshot() -> CatalogStore:
    store = CatalogStore()
    store.reload(lambda: _small_snapshot())
    return store


def _client(store: CatalogStore | None = None, **app_kwargs):
    if store is None:
        store = _store_with_snapshot()
    app = create_app(store, **app_kwargs)
    app.config["TESTING"] = True
    return app.test_client(), store


def _loved_payload(tmdb_id: int = 10) -> dict:
    return {"loved": [{"tmdb_id": tmdb_id, "media_type": "MOVIE"}]}


def test_parse_returns_update_request():
    body = {
        "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
        "media_type": "TV",
        "limit": 5,
        "min_score": 0.5,
    }
    parsed = parse_update_request(body)
    assert isinstance(parsed, UpdateRequest)
    assert parsed.loved == [(10, "MOVIE")]
    assert parsed.media_type == "TV"
    assert parsed.limit == 5
    assert parsed.min_score == 0.5
    assert parsed.exclude == []
    assert parsed.genre_include == []
    assert parsed.genre_exclude == []


def test_parse_min_score_absent_or_null_is_none():
    base = _loved_payload()
    assert parse_update_request(base).min_score is None
    assert parse_update_request({**base, "min_score": None}).min_score is None


def test_parse_min_score_bounds_accepted():
    base = _loved_payload()
    assert parse_update_request({**base, "min_score": 0}).min_score == 0.0
    assert parse_update_request({**base, "min_score": 1}).min_score == 1.0
    assert parse_update_request({**base, "min_score": 0.25}).min_score == 0.25


@pytest.mark.parametrize(
    "bad_score",
    [-0.1, 1.1, "0.5", True, float("nan"), float("inf")],
)
def test_parse_min_score_rejects_invalid(bad_score):
    with pytest.raises(ValueError, match="min_score must be a number between 0 and 1"):
        parse_update_request({**_loved_payload(), "min_score": bad_score})


def test_parse_min_score_rejects_huge_int():
    with pytest.raises(ValueError, match="min_score must be a number between 0 and 1"):
        parse_update_request({**_loved_payload(), "min_score": 10**400})


def test_parse_tmdb_id_zero_rejected():
    with pytest.raises(ValueError, match="tmdb_id out of range"):
        parse_update_request(
            {"loved": [{"tmdb_id": 0, "media_type": "MOVIE"}]}
        )


def test_parse_tmdb_id_negative_rejected():
    with pytest.raises(ValueError, match="tmdb_id out of range"):
        parse_update_request(
            {"loved": [{"tmdb_id": -1, "media_type": "MOVIE"}]}
        )


def test_parse_defaults_limit_and_media_type():
    parsed = parse_update_request(_loved_payload())
    assert parsed.limit == DEFAULT_LIMIT
    assert parsed.media_type is None
    assert parsed.min_score is None


def test_missing_token_gives_401():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]},
    )
    assert response.status_code == 401
    assert response.get_json() == {"error": "unauthorized"}


def test_wrong_token_gives_401():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]},
        headers=_auth("wrong"),
    )
    assert response.status_code == 401
    assert response.get_json() == {"error": "unauthorized"}


def test_valid_token_works():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]},
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 200
    body = response.get_json()
    assert isinstance(body, list)
    assert body
    assert body[0]["tmdb_id"] == 20


def test_bool_tmdb_id_gives_400():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={"loved": [{"tmdb_id": True, "media_type": "MOVIE"}]},
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {"error": "tmdb_id must be an int"}


def test_limit_at_max_accepted():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "limit": 100,
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 200


def test_limit_above_max_gives_400():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "limit": 101,
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {"error": "limit out of range"}


def test_empty_store_update_and_ready_503():
    store = CatalogStore()
    client, _ = _client(store=store)
    update = client.post(
        "/rec/update",
        json={"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]},
        headers=_auth(UPDATE_TOKEN),
    )
    assert update.status_code == 503
    ready = client.get("/ready")
    assert ready.status_code == 503
    assert ready.get_json() == {"status": "loading"}
    health = client.get("/health")
    assert health.status_code == 200
    assert health.get_json() == {"status": "ok"}


def test_health_no_auth_required():
    client, _ = _client()
    response = client.get("/health")
    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}


def test_ready_with_snapshot():
    client, store = _client()
    response = client.get("/ready")
    assert response.status_code == 200
    assert response.get_json() == {
        "status": "ok",
        "rows": int(store.get().tmdb_ids.shape[0]),
    }


def test_rate_limit_returns_429():
    client, _ = _client(update_rate_limit="2 per minute")
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]}
    headers = _auth(UPDATE_TOKEN)
    assert client.post("/rec/update", json=payload, headers=headers).status_code == 200
    assert client.post("/rec/update", json=payload, headers=headers).status_code == 200
    limited = client.post("/rec/update", json=payload, headers=headers)
    assert limited.status_code == 429
    assert limited.get_json() == {"error": "rate limit exceeded"}
    assert "Retry-After" in limited.headers


def test_retry_after_is_time_until_reset_not_window():
    import time

    client, _ = _client(update_rate_limit="2 per minute")
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]}
    headers = _auth(UPDATE_TOKEN)
    assert client.post("/rec/update", json=payload, headers=headers).status_code == 200
    assert client.post("/rec/update", json=payload, headers=headers).status_code == 200
    first = client.post("/rec/update", json=payload, headers=headers)
    assert first.status_code == 429
    first_retry = int(first.headers["Retry-After"])
    time.sleep(2)
    second = client.post("/rec/update", json=payload, headers=headers)
    assert second.status_code == 429
    assert second.get_json() == {"error": "rate limit exceeded"}
    second_retry = int(second.headers["Retry-After"])
    assert second_retry <= first_retry - 2


def test_http_exception_returns_json_404():
    client, _ = _client()
    response = client.get("/no-such-route")
    assert response.status_code == 404
    assert response.get_json() == {"error": "not found"}


def test_http_exception_returns_json_413():
    client, _ = _client()
    oversized = b"x" * (MAX_CONTENT_LENGTH + 1)
    response = client.post(
        "/rec/update",
        data=oversized,
        headers={
            **_auth(UPDATE_TOKEN),
            "Content-Type": "application/json",
            "Content-Length": str(len(oversized)),
        },
    )
    assert response.status_code == 413
    assert response.get_json() == {"error": "request entity too large"}


def test_unauthorized_does_not_spend_update_rate_limit():
    client, _ = _client(update_rate_limit="2 per minute")
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]}
    wrong = _auth("wrong")
    for _ in range(5):
        assert (
            client.post("/rec/update", json=payload, headers=wrong).status_code
            == 401
        )
    ok = client.post(
        "/rec/update",
        json=payload,
        headers=_auth(UPDATE_TOKEN),
    )
    assert ok.status_code == 200


def test_reload_rate_limit_returns_429(monkeypatch):
    import routes as routes_mod

    monkeypatch.setattr(
        routes_mod,
        "load_snapshot",
        lambda _url: _small_snapshot(),
    )
    client, _ = _client(reload_rate_limit="2 per minute")
    headers = _auth(RELOAD_TOKEN)
    assert client.post("/admin/reload-catalog", headers=headers).status_code == 200
    assert client.post("/admin/reload-catalog", headers=headers).status_code == 200
    limited = client.post("/admin/reload-catalog", headers=headers)
    assert limited.status_code == 429
    assert limited.get_json() == {"error": "rate limit exceeded"}


def test_unauthorized_does_not_spend_reload_rate_limit(monkeypatch):
    import routes as routes_mod

    monkeypatch.setattr(
        routes_mod,
        "load_snapshot",
        lambda _url: _small_snapshot(),
    )
    client, _ = _client(reload_rate_limit="2 per minute")
    wrong = _auth("wrong")
    for _ in range(5):
        assert (
            client.post("/admin/reload-catalog", headers=wrong).status_code == 401
        )
    ok = client.post(
        "/admin/reload-catalog",
        headers=_auth(RELOAD_TOKEN),
    )
    assert ok.status_code == 200


def test_reload_loader_error_keeps_old_snapshot(monkeypatch):
    store = CatalogStore()
    store.reload(lambda: _small_snapshot())
    before = store.get()
    assert before is not None

    import routes as routes_mod

    def raising_loader(_url: str):
        raise RuntimeError("db down")

    monkeypatch.setattr(routes_mod, "load_snapshot", raising_loader)

    client, _ = _client(store=store)
    response = client.post(
        "/admin/reload-catalog",
        headers=_auth(RELOAD_TOKEN),
    )
    assert response.status_code == 500
    assert response.get_json() == {"error": "reload failed"}
    assert store.get() is before


def test_min_score_filters_known_cosine_scores():
    # Loved [1,0,0,0]: id 20 score 0.8, id 40 score 0.6, id 30 score 0.0
    client, _ = _client()
    headers = _auth(UPDATE_TOKEN)
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}], "limit": 10}

    default = client.post("/rec/update", json=payload, headers=headers)
    assert default.status_code == 200
    default_ids = {row["tmdb_id"] for row in default.get_json()}
    assert 20 in default_ids
    assert 40 in default_ids

    none_score = client.post(
        "/rec/update",
        json={**payload, "min_score": None},
        headers=headers,
    )
    assert none_score.status_code == 200
    assert {row["tmdb_id"] for row in none_score.get_json()} == default_ids

    high = client.post(
        "/rec/update",
        json={**payload, "min_score": 0.7},
        headers=headers,
    )
    assert high.status_code == 200
    high_ids = {row["tmdb_id"] for row in high.get_json()}
    assert 20 in high_ids
    assert 40 not in high_ids


def test_min_score_zero_and_one_accepted():
    client, _ = _client()
    headers = _auth(UPDATE_TOKEN)
    base = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}]}
    assert (
        client.post(
            "/rec/update", json={**base, "min_score": 0}, headers=headers
        ).status_code
        == 200
    )
    assert (
        client.post(
            "/rec/update", json={**base, "min_score": 1}, headers=headers
        ).status_code
        == 200
    )


@pytest.mark.parametrize(
    "bad_score",
    [-0.1, 1.1, "0.5", True, float("nan")],
)
def test_min_score_invalid_gives_400(bad_score):
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "min_score": bad_score,
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {
        "error": "min_score must be a number between 0 and 1"
    }


def test_exclude_rank1_still_fills_limit():
    # min_score=0 keeps enough candidates so exclude-before-topk can refill.
    client, _ = _client()
    headers = _auth(UPDATE_TOKEN)
    base = {
        "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
        "limit": 2,
        "min_score": 0,
    }
    baseline = client.post("/rec/update", json=base, headers=headers)
    assert baseline.status_code == 200
    baseline_body = baseline.get_json()
    assert len(baseline_body) == 2
    rank1_id = baseline_body[0]["tmdb_id"]
    rank1_type = baseline_body[0]["media_type"]

    excluded = client.post(
        "/rec/update",
        json={
            **base,
            "exclude": [{"tmdb_id": rank1_id, "media_type": rank1_type}],
        },
        headers=headers,
    )
    assert excluded.status_code == 200
    body = excluded.get_json()
    assert len(body) == 2
    assert all(row["tmdb_id"] != rank1_id for row in body)


def test_no_exclude_same_as_today():
    client, _ = _client()
    headers = _auth(UPDATE_TOKEN)
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}], "limit": 10}
    without = client.post("/rec/update", json=payload, headers=headers)
    with_empty = client.post(
        "/rec/update",
        json={**payload, "exclude": []},
        headers=headers,
    )
    with_null = client.post(
        "/rec/update",
        json={**payload, "exclude": None},
        headers=headers,
    )
    assert without.status_code == 200
    assert with_empty.status_code == 200
    assert with_null.status_code == 200
    assert without.get_json() == with_empty.get_json()
    assert without.get_json() == with_null.get_json()


def test_exclude_bad_item_gives_400():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "exclude": [{"tmdb_id": True, "media_type": "MOVIE"}],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {"error": "tmdb_id must be an int"}


def test_exclude_over_cap_gives_400(monkeypatch):
    import request_parsing as parsing

    monkeypatch.setattr(parsing, "MAX_EXCLUDE", 2)
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "exclude": [
                {"tmdb_id": 1, "media_type": "MOVIE"},
                {"tmdb_id": 2, "media_type": "MOVIE"},
                {"tmdb_id": 3, "media_type": "MOVIE"},
            ],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {"error": "exclude length out of range"}


def test_exclude_unknown_id_ignored():
    client, _ = _client()
    headers = _auth(UPDATE_TOKEN)
    payload = {"loved": [{"tmdb_id": 10, "media_type": "MOVIE"}], "limit": 10}
    baseline = client.post("/rec/update", json=payload, headers=headers)
    unknown = client.post(
        "/rec/update",
        json={
            **payload,
            "exclude": [{"tmdb_id": 999999, "media_type": "MOVIE"}],
        },
        headers=headers,
    )
    assert baseline.status_code == 200
    assert unknown.status_code == 200
    assert unknown.get_json() == baseline.get_json()


def test_parse_genre_filters_defaults_empty():
    parsed = parse_update_request(_loved_payload())
    assert parsed.genre_include == []
    assert parsed.genre_exclude == []
    assert parse_update_request(
        {**_loved_payload(), "genre_include": None}
    ).genre_include == []
    assert parse_update_request(
        {**_loved_payload(), "genre_exclude": []}
    ).genre_exclude == []


def test_parse_genre_filters_accepted():
    parsed = parse_update_request(
        {
            **_loved_payload(),
            "genre_include": [878, 28],
            "genre_exclude": [16],
        }
    )
    assert parsed.genre_include == [878, 28]
    assert parsed.genre_exclude == [16]


def test_parse_genre_overlap_rejected():
    with pytest.raises(
        ValueError, match="genre_include and genre_exclude must not share an id"
    ):
        parse_update_request(
            {
                **_loved_payload(),
                "genre_include": [28],
                "genre_exclude": [28],
            }
        )


@pytest.mark.parametrize(
    "field,bad",
    [
        ("genre_include", [True]),
        ("genre_include", [0]),
        ("genre_include", [-1]),
        ("genre_include", ["878"]),
        ("genre_include", [28, 28]),
        ("genre_exclude", [1.5]),
        ("genre_exclude", "28"),
    ],
)
def test_parse_genre_bad_types_rejected(field, bad):
    with pytest.raises(ValueError):
        parse_update_request({**_loved_payload(), field: bad})


def test_genre_overlap_gives_400():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "genre_include": [28],
            "genre_exclude": [28],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {
        "error": "genre_include and genre_exclude must not share an id"
    }


def test_genre_bad_type_gives_400():
    client, _ = _client()
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "genre_include": [True],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 400
    assert response.get_json() == {
        "error": "genre_include items must be positive ints"
    }


def _genre_route_snapshot() -> CatalogSnapshot:
    from catalog_loader import build_genre_membership

    raw = np.array(
        [
            [1.0, 0.0, 0.0, 0.0],
            [0.8, 0.6, 0.0, 0.0],
            [0.0, 1.0, 0.0, 0.0],
            [0.6, 0.8, 0.0, 0.0],
            [0.05, 1.0, 0.0, 0.0],
        ],
        dtype=np.float32,
    )
    matrix = _l2(raw)
    tmdb_ids = np.array([10, 20, 30, 40, 50], dtype=np.int64)
    media_types = np.array(
        ["MOVIE", "MOVIE", "MOVIE", "TV", "MOVIE"], dtype=object
    )
    index = {
        (10, "MOVIE"): 0,
        (20, "MOVIE"): 1,
        (30, "MOVIE"): 2,
        (40, "TV"): 3,
        (50, "MOVIE"): 4,
    }
    genre_lists = [
        [],
        [878, 28],
        [16],
        [878],
        [878, 16],
    ]
    genre_matrix, genre_col_index = build_genre_membership(genre_lists)
    return CatalogSnapshot(
        tmdb_ids=tmdb_ids,
        media_types=media_types,
        matrix=matrix,
        index=index,
        genre_matrix=genre_matrix,
        genre_col_index=genre_col_index,
    )


def test_genre_exclude_before_topk_fills_limit():
    store = CatalogStore()
    store.reload(lambda: _genre_route_snapshot())
    client, _ = _client(store)
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "limit": 2,
            "min_score": 0,
            "genre_exclude": [16],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 200
    body = response.get_json()
    assert len(body) == 2
    assert all(row["tmdb_id"] not in {30, 50} for row in body)


def test_genre_include_all_via_http():
    store = CatalogStore()
    store.reload(lambda: _genre_route_snapshot())
    client, _ = _client(store)
    response = client.post(
        "/rec/update",
        json={
            "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
            "limit": 10,
            "min_score": 0,
            "genre_include": [878, 28],
        },
        headers=_auth(UPDATE_TOKEN),
    )
    assert response.status_code == 200
    body = response.get_json()
    assert {row["tmdb_id"] for row in body} == {20}
