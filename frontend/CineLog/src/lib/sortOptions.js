/** Placeholder when no user sort is chosen. Radix Select rejects "". */
export const DEFAULT_SORT = "default";

/** Fallback when a sort key has no Movie/TV equivalent. */
export const DEFAULT_DISCOVER_SORT = "popularity.desc";

/**
 * Movie-only discover sort keys → TV equivalents.
 * Shared keys (popularity, vote_average) stay unchanged.
 */
export const DISCOVER_SORT_MOVIE_TO_TV = {
  "primary_release_date.desc": "first_air_date.desc",
  "primary_release_date.asc": "first_air_date.asc",
  "title.asc": "name.asc",
  "title.desc": "name.desc",
};

/**
 * TV-only discover sort keys → Movie equivalents.
 */
export const DISCOVER_SORT_TV_TO_MOVIE = {
  "first_air_date.desc": "primary_release_date.desc",
  "first_air_date.asc": "primary_release_date.asc",
  "name.asc": "title.asc",
  "name.desc": "title.desc",
};

/**
 * TMDB /discover sort_by tokens for Movie vs TV (official enum).
 * Movie date = primary_release_date; TV date = first_air_date.
 * Movie title = title; TV title = name.
 */
export function buildDiscoverSortOptions(mediaType) {
  const isTv = mediaType === "tv";
  const dateKey = isTv ? "first_air_date" : "primary_release_date";
  const titleKey = isTv ? "name" : "title";

  return [
    { label: "Popularity ↓", value: "popularity.desc" },
    { label: "Popularity ↑", value: "popularity.asc" },
    { label: "Rating ↓", value: "vote_average.desc" },
    { label: "Rating ↑", value: "vote_average.asc" },
    { label: "Release date ↓", value: `${dateKey}.desc` },
    { label: "Release date ↑", value: `${dateKey}.asc` },
    { label: "Title A–Z", value: `${titleKey}.asc` },
    { label: "Title Z–A", value: `${titleKey}.desc` },
  ];
}

/**
 * Map a stored discover sort key to the equivalent for targetMediaType.
 * Shared keys pass through. Unknown keys reset to DEFAULT_DISCOVER_SORT.
 */
export function mapDiscoverSortForMediaType(sortValue, targetMediaType) {
  if (sortValue == null || sortValue === "" || sortValue === DEFAULT_SORT) {
    return sortValue;
  }

  const valid = new Set(
    buildDiscoverSortOptions(targetMediaType).map((opt) => opt.value),
  );
  if (valid.has(sortValue)) {
    return sortValue;
  }

  const mapped =
    targetMediaType === "tv"
      ? DISCOVER_SORT_MOVIE_TO_TV[sortValue]
      : DISCOVER_SORT_TV_TO_MOVIE[sortValue];

  if (mapped != null && valid.has(mapped)) {
    return mapped;
  }

  return DEFAULT_DISCOVER_SORT;
}

/**
 * Client-side sort options for the recommendation page.
 */
export const RECOMMENDATION_SORT_OPTIONS = [
  { label: "Best match", value: "match" },
  { label: "Rating", value: "ratingDesc" },
  { label: "Newest", value: "newest" },
  { label: "Oldest", value: "oldest" },
  { label: "Title A to Z", value: "titleAsc" },
];
