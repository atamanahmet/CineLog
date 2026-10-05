"""Immutable in-memory catalog embedding snapshot."""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np


def empty_genre_membership(rows: int) -> tuple[np.ndarray, dict[int, int]]:
    """Zero-width genre matrix for snapshots without genre data."""
    return np.zeros((rows, 0), dtype=bool), {}


@dataclass(frozen=True)
class CatalogSnapshot:
    tmdb_ids: np.ndarray
    media_types: np.ndarray
    matrix: np.ndarray
    index: dict[tuple[int, str], int]
    genre_matrix: np.ndarray
    genre_col_index: dict[int, int]

    def __post_init__(self) -> None:
        rows = int(self.matrix.shape[0])
        if rows < 1:
            raise ValueError("catalog snapshot requires at least one row")
        if not (
            rows
            == len(self.tmdb_ids)
            == len(self.media_types)
            == len(self.index)
            == int(self.genre_matrix.shape[0])
        ):
            raise ValueError(
                "catalog snapshot length mismatch: "
                f"matrix={rows} tmdb_ids={len(self.tmdb_ids)} "
                f"media_types={len(self.media_types)} index={len(self.index)} "
                f"genre_matrix={int(self.genre_matrix.shape[0])}"
            )
        self.matrix.setflags(write=False)
        self.tmdb_ids.setflags(write=False)
        self.media_types.setflags(write=False)
        self.genre_matrix.setflags(write=False)
