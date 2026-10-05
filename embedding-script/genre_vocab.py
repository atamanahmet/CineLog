"""Canonical TMDB genre id mapping for movie and TV."""

from __future__ import annotations

# Movie genre ids are canonical and map to themselves.
MOVIE_GENRE_IDS: frozenset[int] = frozenset(
    {
        28,
        12,
        16,
        35,
        80,
        99,
        18,
        10751,
        14,
        36,
        27,
        10402,
        9648,
        10749,
        878,
        10770,
        53,
        10752,
        37,
    }
)

# TV ids that expand or remap onto canonical movie ids.
TV_EXPAND: dict[int, tuple[int, ...]] = {
    10759: (28, 12),
    10765: (878, 14),
    10768: (10752,),
    10762: (10751,),
}

# TV ids that stay as themselves (shared with movie or TV-only).
TV_SELF_IDS: frozenset[int] = frozenset(
    {
        16,
        35,
        80,
        99,
        18,
        10751,
        9648,
        37,
        10763,
        10764,
        10766,
        10767,
    }
)

KNOWN_GENRE_IDS: frozenset[int] = frozenset(
    set(MOVIE_GENRE_IDS)
    | set(TV_EXPAND)
    | {canon for values in TV_EXPAND.values() for canon in values}
    | set(TV_SELF_IDS)
)

# Display names for the 19 movie genres plus TV-only ids.
GENRE_NAMES: dict[int, str] = {
    28: "Action",
    12: "Adventure",
    16: "Animation",
    35: "Comedy",
    80: "Crime",
    99: "Documentary",
    18: "Drama",
    10751: "Family",
    14: "Fantasy",
    36: "History",
    27: "Horror",
    10402: "Music",
    9648: "Mystery",
    10749: "Romance",
    878: "Science Fiction",
    10770: "TV Movie",
    53: "Thriller",
    10752: "War",
    37: "Western",
    10763: "News",
    10764: "Reality",
    10766: "Soap",
    10767: "Talk",
}


def genre_name(genre_id: int) -> str:
    """Return the display name for one canonical genre id."""
    return GENRE_NAMES[genre_id]


def to_canonical(media_type: str, genre_ids: list[int]) -> list[int]:
    """Return sorted unique canonical genre ids for one media type."""
    if media_type == "MOVIE":
        return sorted(set(int(g) for g in genre_ids))
    if media_type == "TV":
        out: set[int] = set()
        for raw in genre_ids:
            gid = int(raw)
            if gid in TV_EXPAND:
                out.update(TV_EXPAND[gid])
            else:
                out.add(gid)
        return sorted(out)
    raise ValueError(f"unsupported media_type: {media_type}")
