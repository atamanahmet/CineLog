"""Read-only neighbor sanity check via the running rec-engine."""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any

import numpy as np
import requests
from dotenv import load_dotenv

from tmdb_source import create_session
from verify_vectors import decode, fetch_title, read_rows

QUERY_MOVIES = 3
QUERY_TV = 2
NEIGHBOR_LIMIT = 5
SCORE_TOLERANCE = 1e-3
DISPLAY_THRESHOLD = 0.3
HTTP_TIMEOUT_S = 10
DEFAULT_ENGINE_BASE_URL = "http://localhost:8181"
RNG_SEED = 0


class NeighborCheckError(Exception):
    """Fatal check_neighbors failure; main exits with the given code."""

    def __init__(self, message: str, exit_code: int = 2) -> None:
        super().__init__(message)
        self.exit_code = exit_code


def read_env() -> dict[str, str]:
    """Load TMDB/DB from dotenv; UPDATE_TOKEN from process env only."""
    process_token = os.environ.get("UPDATE_TOKEN")
    if process_token is None or not process_token.strip():
        raise NeighborCheckError("Missing required environment variable: UPDATE_TOKEN")
    update_token = process_token.strip()

    load_dotenv()

    values: dict[str, str] = {"UPDATE_TOKEN": update_token}
    for name in ("TMDB_API_KEY", "REC_WRITE_DATABASE_URL"):
        raw = os.environ.get(name)
        if raw is None or not raw.strip():
            raise NeighborCheckError(f"Missing required environment variable: {name}")
        values[name] = raw.strip()

    engine = os.environ.get("REC_ENGINE_BASE_URL")
    values["REC_ENGINE_BASE_URL"] = (
        engine.strip() if engine and engine.strip() else DEFAULT_ENGINE_BASE_URL
    )
    return values


def pick_query_keys(
    rows: list[tuple[int, str, np.ndarray]],
) -> list[tuple[int, str]]:
    """Pick 3 MOVIE + 2 TV with fixed seed, then sort for stable order."""
    movies = [(i, t) for i, t, _ in rows if t == "MOVIE"]
    tvs = [(i, t) for i, t, _ in rows if t == "TV"]
    if len(movies) < QUERY_MOVIES or len(tvs) < QUERY_TV:
        raise NeighborCheckError(
            f"need at least {QUERY_MOVIES} MOVIE and {QUERY_TV} TV rows, "
            f"got {len(movies)} MOVIE and {len(tvs)} TV"
        )
    rng = np.random.default_rng(RNG_SEED)
    movie_picks = [movies[i] for i in rng.choice(len(movies), size=QUERY_MOVIES, replace=False)]
    tv_picks = [tvs[i] for i in rng.choice(len(tvs), size=QUERY_TV, replace=False)]
    return sorted(movie_picks + tv_picks, key=lambda k: (k[1], k[0]))


def call_rec_update(
    engine_base_url: str,
    update_token: str,
    query_key: tuple[int, str],
) -> list[dict[str, Any]]:
    """POST /rec/update for one loved key."""
    url = engine_base_url.rstrip("/") + "/rec/update"
    payload = {
        "loved": [{"tmdb_id": query_key[0], "media_type": query_key[1]}],
        "limit": NEIGHBOR_LIMIT,
    }
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {update_token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT_S) as response:
            status = response.getcode()
            raw = response.read()
    except urllib.error.HTTPError as exc:
        raise NeighborCheckError(f"engine HTTP {exc.code}: {exc.reason}") from exc
    except urllib.error.URLError as exc:
        reason = getattr(exc, "reason", exc)
        raise NeighborCheckError(f"engine unreachable: {reason}") from exc
    except TimeoutError as exc:
        raise NeighborCheckError("engine HTTP timeout: request timed out") from exc

    if status != 200:
        raise NeighborCheckError(f"engine HTTP {status}: unexpected status")

    try:
        data = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise NeighborCheckError(f"engine HTTP {status}: invalid JSON body") from exc

    if not isinstance(data, list):
        raise NeighborCheckError(f"engine HTTP {status}: body is not a list")
    return data


def local_cosine_scores(
    rows: list[tuple[int, str, np.ndarray]],
    query_key: tuple[int, str],
) -> dict[tuple[int, str], float]:
    """Cosine of the query row against every other stored vector."""
    query_vec: np.ndarray | None = None
    others: list[tuple[tuple[int, str], np.ndarray]] = []
    for tmdb_id, media_type, vector in rows:
        key = (tmdb_id, media_type)
        if key == query_key:
            query_vec = vector
        else:
            others.append((key, vector))
    if query_vec is None:
        raise NeighborCheckError(
            f"query key {query_key[0]} {query_key[1]} missing from rows"
        )

    scores: dict[tuple[int, str], float] = {}
    for key, vector in others:
        scores[key] = float(np.dot(query_vec, vector))
    return scores


def compare_results(
    engine_results: list[dict[str, Any]],
    local_scores: dict[tuple[int, str], float],
    query_key: tuple[int, str],
) -> list[str]:
    """Return failure messages for engine vs local neighbor agreement."""
    failures: list[str] = []
    result_keys = [
        (int(row["tmdb_id"]), str(row["media_type"])) for row in engine_results
    ]

    if query_key in result_keys:
        failures.append(
            f"query key {query_key[0]} {query_key[1]} appears in results"
        )

    engine_scores = [float(row["score"]) for row in engine_results]
    if engine_scores != sorted(engine_scores, reverse=True):
        failures.append("results not sorted by score descending")

    for row, key in zip(engine_results, result_keys):
        if key not in local_scores:
            failures.append(f"{key[0]} {key[1]} missing from local scores")
            continue
        engine_score = float(row["score"])
        local_score = local_scores[key]
        if abs(engine_score - local_score) > SCORE_TOLERANCE:
            failures.append(
                f"{key[0]} {key[1]} score mismatch "
                f"engine={engine_score:.6f} local={local_score:.6f}"
            )

    if engine_results:
        lowest = min(float(row["score"]) for row in engine_results)
        returned = set(result_keys)
        for key, local_score in local_scores.items():
            if key in returned:
                continue
            if local_score > lowest + SCORE_TOLERANCE:
                failures.append(
                    f"missing candidate {key[0]} {key[1]} "
                    f"local={local_score:.6f} above lowest returned {lowest:.6f}"
                )

    return failures


def title_or_unknown(session, media_type: str, tmdb_id: int) -> str:
    found = fetch_title(session, media_type, tmdb_id)
    if found is None:
        return "?"
    title, _overview = found
    return title or "?"


def print_query_block(
    session,
    query_key: tuple[int, str],
    engine_results: list[dict[str, Any]],
    local_scores: dict[tuple[int, str], float],
) -> None:
    q_id, q_type = query_key
    q_title = title_or_unknown(session, q_type, q_id)
    print(f"query: {q_title} ({q_type} {q_id})")

    for rank, row in enumerate(engine_results, start=1):
        n_id = int(row["tmdb_id"])
        n_type = str(row["media_type"])
        n_title = title_or_unknown(session, n_type, n_id)
        engine_score = float(row["score"])
        local_score = local_scores.get((n_id, n_type), float("nan"))
        print(
            f"  {rank}. {n_title} ({n_type}) "
            f"engine={engine_score:.6f} local={local_score:.6f}"
        )

    values = np.asarray(list(local_scores.values()), dtype=np.float64)
    if values.size == 0:
        print("  local_all: empty")
        return
    above = int(np.sum(values >= DISPLAY_THRESHOLD))
    print(
        f"  local_all: min={values.min():.6f} "
        f"median={np.median(values):.6f} max={values.max():.6f} "
        f"ge_{DISPLAY_THRESHOLD}={above} (display only)"
    )


def main() -> None:
    try:
        env = read_env()
        rows = read_rows(env["REC_WRITE_DATABASE_URL"])
        keys = pick_query_keys(rows)
        session = create_session(env["TMDB_API_KEY"])

        all_failures: list[str] = []
        for key in keys:
            engine_results = call_rec_update(
                env["REC_ENGINE_BASE_URL"], env["UPDATE_TOKEN"], key
            )
            scores = local_cosine_scores(rows, key)
            try:
                print_query_block(session, key, engine_results, scores)
            except requests.RequestException as exc:
                raise NeighborCheckError(f"TMDB unreachable: {exc}") from exc

            for message in compare_results(engine_results, scores, key):
                all_failures.append(f"{key[1]} {key[0]}: {message}")

        if all_failures:
            for message in all_failures:
                print(f"FAIL: {message}")
            print("RESULT: FAIL")
            raise NeighborCheckError("RESULT: FAIL", exit_code=1)

        print("RESULT: PASS")
    except NeighborCheckError as exc:
        if str(exc) and str(exc) != "RESULT: FAIL":
            print(str(exc))
        sys.exit(exc.exit_code)

    sys.exit(0)


if __name__ == "__main__":
    main()
