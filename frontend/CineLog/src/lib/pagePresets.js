/**
 * Page catalog presets. Filter values here are sent explicitly when user has no override.
 */
export const pagePresets = {
  discover: {
    sort: null,
    filters: {},
  },
  new: {
    sort: "release_date.desc",
    filters: {},
  },
  newReleases: {
    sort: "release_date.desc",
    filters: {},
  },
  top: {
    sort: "vote_average.desc",
    filtersByMediaType: {
      movie: { voteCount: 1000 },
      tv: { voteCount: 1000 },
    },
  },
  upcoming: {
    sort: "popularity.desc",
    upcoming: true,
    filtersByMediaType: {
      movie: { voteCount: 0 },
      tv: { voteCount: 0 },
    },
  },
  search: {
    sort: null,
    filters: {},
  },
  watchlist: {
    sort: null,
    filters: {},
  },
  watched: {
    sort: null,
    filters: {},
  },
  Watchlist: {
    sort: null,
    filters: {},
  },
  Watchedlist: {
    sort: null,
    filters: {},
  },
  Loved: {
    sort: null,
    filters: {},
  },
  Recommendation: {
    sort: null,
    filters: {},
  },
  "Find similar": {
    sort: null,
    filters: {},
  },
};

/**
 * Map a location pathname to a pagePresets key.
 */
export function pageKeyFromPath(pathname) {
  const path = (pathname ?? "").replace(/\/+$/, "") || "/";
  if (path === "/") {
    return "discover";
  }
  if (path === "/new") {
    return "newReleases";
  }
  if (path === "/top") {
    return "top";
  }
  if (path === "/upcoming") {
    return "upcoming";
  }
  if (path === "/search") {
    return "search";
  }
  if (path === "/profile") {
    return "watchlist";
  }
  return "discover";
}

/**
 * Preset filter fields for one page and media type.
 */
export function getPresetFilters(pageKey, mediaType) {
  const preset = pagePresets[pageKey] ?? pagePresets.discover;
  if (preset.filtersByMediaType) {
    return { ...(preset.filtersByMediaType[mediaType] ?? {}) };
  }
  return { ...(preset.filters ?? {}) };
}

/**
 * Preset default sort for one page, or null.
 */
export function getPresetSort(pageKey) {
  const preset = pagePresets[pageKey] ?? pagePresets.discover;
  return preset.sort ?? null;
}

/**
 * True when this page preset sends the discover upcoming flag.
 */
export function getPresetUpcoming(pageKey) {
  return Boolean((pagePresets[pageKey] ?? pagePresets.discover).upcoming);
}
