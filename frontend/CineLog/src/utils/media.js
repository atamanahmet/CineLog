/**
 * Shared releaseDate for movie and TV.
 */
export function mediaReleaseDate(item) {
  return item?.releaseDate || null;
}

/**
 * Calendar year from shared releaseDate.
 */
export function mediaReleaseYear(item) {
  const dateStr = mediaReleaseDate(item);
  if (!dateStr) {
    return "Unknown";
  }
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) {
    return "Unknown";
  }
  return d.getFullYear();
}

export function resolveMediaType(item, hint) {
  if (hint === true || hint === "tv") {
    return "tv";
  }
  if (hint === false || hint === "movie") {
    return "movie";
  }
  const raw = item?.media_type ?? item?.mediaType;
  if (typeof raw === "string") {
    const lower = raw.toLowerCase();
    if (lower === "tv") {
      return "tv";
    }
    if (lower === "movie") {
      return "movie";
    }
  }
  console.error(
    "resolveMediaType: no hint or mediaType field on item, defaulting to movie",
    item,
  );
  return "movie";
}

/**
 * Pick first defined value (keeps 0 and false).
 */
function firstDefined(...values) {
  for (const value of values) {
    if (value !== undefined) {
      return value;
    }
  }
  return undefined;
}

/**
 * Normalize poster path; blank or "null" string becomes null.
 */
function normalizePosterPath(raw) {
  const path = firstDefined(raw?.posterPath, raw?.poster_path);
  if (path == null || path === "") {
    return null;
  }
  const text = String(path);
  if (text === "null" || text.endsWith("null")) {
    return null;
  }
  return text;
}

/**
 * Normalize vote average; missing becomes null, 0 stays 0.
 */
function normalizeVoteAverage(raw) {
  const value = firstDefined(raw?.voteAverage, raw?.vote_average);
  if (value == null || value === "") {
    return null;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

/**
 * Normalize engine score; missing becomes null, 0 stays 0.
 */
function normalizeScore(raw) {
  const value = firstDefined(raw?.score);
  if (value == null || value === "") {
    return null;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

/**
 * Normalize genre id list. Missing becomes empty, never null.
 */
function normalizeGenreIds(raw) {
  const value = firstDefined(raw?.genreIds, raw?.genre_ids);
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id));
}

/**
 * One canonical media card model from any API or TMDB-shaped payload.
 */
export function normalizeMediaItem(raw) {
  if (raw == null || typeof raw !== "object") {
    return {
      id: null,
      mediaType: "MOVIE",
      title: null,
      originalTitle: null,
      posterPath: null,
      releaseDate: null,
      voteAverage: null,
      overview: null,
      adult: null,
      originalLanguage: null,
      character: null,
      score: null,
      genreIds: [],
    };
  }

  const title = firstDefined(raw.title, raw.name) ?? null;
  const originalTitle =
    firstDefined(
      raw.originalTitle,
      raw.original_title,
      raw.original_name,
      title,
    ) ?? null;
  const releaseDate =
    firstDefined(raw.releaseDate, raw.release_date, raw.first_air_date) ?? null;
  const overview = firstDefined(raw.overview) ?? null;
  const adult = firstDefined(raw.adult) ?? null;
  const originalLanguage =
    firstDefined(raw.originalLanguage, raw.original_language) ?? null;
  const character = firstDefined(raw.character) ?? null;
  const resolved = resolveMediaType(raw);

  return {
    id: firstDefined(raw.id) ?? null,
    mediaType: resolved === "tv" ? "TV" : "MOVIE",
    title,
    originalTitle,
    posterPath: normalizePosterPath(raw),
    releaseDate,
    voteAverage: normalizeVoteAverage(raw),
    overview,
    adult,
    originalLanguage,
    character,
    score: normalizeScore(raw),
    genreIds: normalizeGenreIds(raw),
  };
}

/**
 * Details route path for a movie or TV id.
 */
export function buildDetailsPath(mediaType, id) {
  const type = typeof mediaType === "string" ? mediaType.toLowerCase() : "";
  if (type !== "movie" && type !== "tv") {
    throw new Error(`Invalid media type "${mediaType}"`);
  }
  return `/details/${type}/${id}`;
}

/**
 * Navigate to details with optional placeholder state.
 */
export function navigateToDetails(navigate, media, typeHint) {
  const type = resolveMediaType(media, typeHint);
  navigate(buildDetailsPath(type, media.id), { state: { placeholder: media } });
}

/**
 * Compare two values with nulls always last.
 */
function compareWithNullsLast(a, b, compareValues) {
  const aNull = a == null;
  const bNull = b == null;
  if (aNull && bNull) {
    return 0;
  }
  if (aNull) {
    return 1;
  }
  if (bNull) {
    return -1;
  }
  return compareValues(a, b);
}

/**
 * Merge movie and TV lists by score desc; null scores last; stable ties; no mutate.
 */
export function mergeByScore(movies, tv) {
  const movieList = Array.isArray(movies) ? movies : [];
  const tvList = Array.isArray(tv) ? tv : [];
  const decorated = [
    ...movieList.map((item, index) => ({ item, index })),
    ...tvList.map((item, index) => ({
      item,
      index: movieList.length + index,
    })),
  ];
  decorated.sort((left, right) => {
    const cmp = compareWithNullsLast(
      left.item?.score,
      right.item?.score,
      (a, b) => b - a,
    );
    return cmp !== 0 ? cmp : left.index - right.index;
  });
  return decorated.map((entry) => entry.item);
}

/**
 * True when key is in the activeKeys list.
 */
function hasActiveKey(activeKeys, key) {
  return Array.isArray(activeKeys) && activeKeys.includes(key);
}

/**
 * Calendar year number from releaseDate, or null when unknown.
 */
function itemYear(item) {
  const dateStr = item?.releaseDate;
  if (dateStr == null || dateStr === "") {
    return null;
  }
  const year = Number(String(dateStr).slice(0, 4));
  return Number.isFinite(year) ? year : null;
}

/**
 * True when a two-value range matches the full bounds range.
 */
function isFullRange(range, bound) {
  if (!Array.isArray(range) || range.length !== 2 || bound == null) {
    return false;
  }
  return range[0] === bound.min && range[1] === bound.max;
}

/**
 * Filter media items by visible filter keys only. Unknown values pass at full defaults.
 */
export function filterMediaItems(items, filters, activeKeys, bounds) {
  if (!Array.isArray(items)) {
    return [];
  }
  if (filters == null || !Array.isArray(activeKeys) || activeKeys.length === 0) {
    return items.slice();
  }

  const yearRange = filters.yearRange;
  const rating = filters.rating;
  const languages = filters.languages ?? [];
  const adult = filters.adult;
  const voteCount = filters.voteCount;
  const minRuntime = filters.minRuntime;
  const maxRuntime = filters.maxRuntime;

  const yearActive = hasActiveKey(activeKeys, "yearRange");
  const ratingActive = hasActiveKey(activeKeys, "rating");
  const languagesActive = hasActiveKey(activeKeys, "languages");
  const adultActive = hasActiveKey(activeKeys, "adult");
  const voteActive = hasActiveKey(activeKeys, "voteCount");
  const runtimeActive = hasActiveKey(activeKeys, "runtime");

  const yearNarrowed =
    yearActive &&
    Array.isArray(yearRange) &&
    yearRange.length === 2 &&
    !isFullRange(yearRange, bounds?.yearRange);
  const ratingNarrowed =
    ratingActive &&
    Array.isArray(rating) &&
    rating.length === 2 &&
    !isFullRange(rating, bounds?.rating);
  const languagesNarrowed = languagesActive && languages.length > 0;
  const voteNarrowed =
    voteActive && voteCount != null && Number.isFinite(Number(voteCount));
  const runtimeNarrowed =
    runtimeActive &&
    ((minRuntime != null && Number.isFinite(Number(minRuntime))) ||
      (maxRuntime != null && Number.isFinite(Number(maxRuntime))));
  const runtimeFull =
    bounds?.runtime != null &&
    (minRuntime == null || minRuntime === bounds.runtime.min) &&
    (maxRuntime == null || maxRuntime === bounds.runtime.max);
  const runtimeApplies = runtimeNarrowed && !runtimeFull;

  return items.filter((item) => {
    if (yearNarrowed) {
      const year = itemYear(item);
      if (year == null) {
        return false;
      }
      if (year < yearRange[0] || year > yearRange[1]) {
        return false;
      }
    }

    if (ratingNarrowed) {
      const value = item?.voteAverage;
      if (value == null || !Number.isFinite(Number(value))) {
        return false;
      }
      if (value < rating[0] || value > rating[1]) {
        return false;
      }
    }

    if (languagesNarrowed) {
      const lang = item?.originalLanguage;
      if (lang == null || lang === "") {
        return false;
      }
      if (!languages.includes(lang)) {
        return false;
      }
    }

    if (adultActive && adult !== true) {
      if (item?.adult === true) {
        return false;
      }
    }

    if (voteNarrowed) {
      const votes = item?.voteCount;
      if (votes == null || !Number.isFinite(Number(votes))) {
        return false;
      }
      if (Number(votes) < Number(voteCount)) {
        return false;
      }
    }

    if (runtimeApplies) {
      const runtime = item?.runtime;
      if (runtime == null || !Number.isFinite(Number(runtime))) {
        return false;
      }
      const low = minRuntime != null ? Number(minRuntime) : bounds.runtime.min;
      const high = maxRuntime != null ? Number(maxRuntime) : bounds.runtime.max;
      if (runtime < low || runtime > high) {
        return false;
      }
    }

    return true;
  });
}

/**
 * Sort media items by key. Returns a new array; never mutates input.
 */
export function sortMediaItems(items, sortKey) {
  if (!Array.isArray(items)) {
    return [];
  }
  if (sortKey == null || sortKey === "match") {
    return items.slice();
  }

  const decorated = items.map((item, index) => ({ item, index }));
  decorated.sort((left, right) => {
    let cmp = 0;
    switch (sortKey) {
      case "ratingDesc":
        cmp = compareWithNullsLast(
          left.item?.voteAverage,
          right.item?.voteAverage,
          (a, b) => b - a,
        );
        break;
      case "newest":
        cmp = compareWithNullsLast(
          left.item?.releaseDate,
          right.item?.releaseDate,
          (a, b) => String(b).localeCompare(String(a)),
        );
        break;
      case "oldest":
        cmp = compareWithNullsLast(
          left.item?.releaseDate,
          right.item?.releaseDate,
          (a, b) => String(a).localeCompare(String(b)),
        );
        break;
      case "titleAsc":
        cmp = String(left.item?.title ?? "")
          .toLowerCase()
          .localeCompare(String(right.item?.title ?? "").toLowerCase());
        break;
      default:
        cmp = 0;
        break;
    }
    return cmp !== 0 ? cmp : left.index - right.index;
  });
  return decorated.map((entry) => entry.item);
}
