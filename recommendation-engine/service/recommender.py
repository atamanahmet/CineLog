"""Score catalog rows against a loved-item centroid."""

from __future__ import annotations

import numpy as np

from catalog import CatalogSnapshot
from config import SIMILARITY_THRESHOLD

# Discover with_genres joins ids with commas (TMDB AND). Include needs every id.
GENRE_INCLUDE_MODE = "all"


def build_exclude_mask(
    exclude: list[tuple[int, str]],
    index: dict[tuple[int, str], int],
    n: int,
) -> np.ndarray:
    """Build a boolean row mask from exclude keys; unknown ids ignored."""
    mask = np.zeros(n, dtype=bool)
    for key in exclude:
        row = index.get(key)
        if row is not None:
            mask[row] = True
    return mask


def build_genre_keep_mask(
    snapshot: CatalogSnapshot,
    genre_include: list[int],
    genre_exclude: list[int],
) -> np.ndarray | None:
    """
    Keep-mask for genre filters. None when both lists are empty.
    Include (ALL): row must have every listed id. Unknown include id → none pass.
    Exclude (ANY): drop rows that have any listed id. Unknown exclude id → no-op.
    """
    if not genre_include and not genre_exclude:
        return None

    n = int(snapshot.matrix.shape[0])
    keep = np.ones(n, dtype=bool)

    if genre_include:
        if GENRE_INCLUDE_MODE != "all":
            raise RuntimeError(f"unsupported GENRE_INCLUDE_MODE={GENRE_INCLUDE_MODE!r}")
        for gid in genre_include:
            col = snapshot.genre_col_index.get(gid)
            if col is None:
                return np.zeros(n, dtype=bool)
            keep &= snapshot.genre_matrix[:, col]

    if genre_exclude:
        hit = np.zeros(n, dtype=bool)
        for gid in genre_exclude:
            col = snapshot.genre_col_index.get(gid)
            if col is not None:
                hit |= snapshot.genre_matrix[:, col]
        keep &= ~hit

    return keep


def recommend(
    snapshot: CatalogSnapshot,
    loved: list[tuple[int, str]],
    media_type: str | None,
    limit: int,
    min_score: float | None = None,
    exclude_mask: np.ndarray | None = None,
    genre_include: list[int] | None = None,
    genre_exclude: list[int] | None = None,
) -> list[dict]:
    unique_loved = set(loved)
    row_indices = [
        snapshot.index[key]
        for key in unique_loved
        if key in snapshot.index
    ]
    if not row_indices:
        return []

    loved_rows = snapshot.matrix[np.asarray(row_indices, dtype=np.int64)]
    centroid = np.mean(loved_rows, axis=0, dtype=np.float32)
    norm = float(np.linalg.norm(centroid))
    if norm == 0.0:
        return []
    centroid = centroid / np.float32(norm)

    scores = snapshot.matrix @ centroid

    n = scores.shape[0]
    exclude = np.zeros(n, dtype=bool)
    exclude[np.asarray(row_indices, dtype=np.int64)] = True
    if exclude_mask is not None:
        exclude |= exclude_mask

    threshold = SIMILARITY_THRESHOLD if min_score is None else min_score
    mask = (~exclude) & (scores >= threshold)
    if media_type is not None:
        mask &= snapshot.media_types == media_type

    genre_keep = build_genre_keep_mask(
        snapshot,
        genre_include or [],
        genre_exclude or [],
    )
    if genre_keep is not None:
        mask &= genre_keep

    candidate_idx = np.flatnonzero(mask)
    if candidate_idx.size == 0:
        return []

    k = min(limit, int(candidate_idx.size))
    candidate_scores = scores[candidate_idx]
    part = np.argpartition(candidate_scores, -k)[-k:]
    selected = candidate_idx[part]
    order = np.lexsort((selected, -scores[selected]))
    selected = selected[order]

    return [
        {
            "tmdb_id": int(snapshot.tmdb_ids[i]),
            "media_type": str(snapshot.media_types[i]),
            "score": float(scores[i]),
        }
        for i in selected
    ]
