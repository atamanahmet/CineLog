import { getPresetFilters } from "./pagePresets";
import { emptyTriState, sanitizeTriState } from "./triStateFilter";

/**
 * Clamp a number into [min, max]. Non-finite values become min.
 */
export function clampFilterValue(value, min, max) {
  const n = Number(value);
  if (!Number.isFinite(n)) {
    return min;
  }
  if (n < min) {
    return min;
  }
  if (n > max) {
    return max;
  }
  return n;
}

/**
 * Empty user filter state. voteCount and runtime stay null (backend or preset fills).
 */
export function emptyFilters(bounds) {
  return {
    genreFilter: emptyTriState(),
    languageFilter: emptyTriState(),
    yearRange: [bounds.yearRange.min, bounds.yearRange.max],
    rating: [bounds.rating.min, bounds.rating.max],
    languages: [],
    adult: false,
    voteCount: null,
    minRuntime: null,
    maxRuntime: null,
  };
}

/**
 * Filter state after Reset. Clears overrides so the page preset can apply.
 */
export function filtersAfterReset(bounds) {
  return emptyFilters(bounds);
}

/**
 * Soft empty shape used before discover defaults load.
 */
export function emptyFiltersSoft() {
  return {
    genreFilter: emptyTriState(),
    languageFilter: emptyTriState(),
    yearRange: null,
    rating: null,
    languages: [],
    adult: false,
    voteCount: null,
    minRuntime: null,
    maxRuntime: null,
  };
}

/**
 * Sanitize one two-value numeric range, or fall back to that field default.
 */
function sanitizeRange(value, fallback, min, max) {
  if (!Array.isArray(value) || value.length !== 2) {
    return [...fallback];
  }
  let low = Number(value[0]);
  let high = Number(value[1]);
  if (!Number.isFinite(low) || !Number.isFinite(high)) {
    return [...fallback];
  }
  low = clampFilterValue(low, min, max);
  high = clampFilterValue(high, min, max);
  if (low > high) {
    return [high, low];
  }
  return [low, high];
}

/**
 * Sanitize a nullable numeric filter. Null stays null. Invalid becomes null.
 */
function sanitizeNullableNumber(value, min, max) {
  if (value == null || value === "") {
    return null;
  }
  const n = Number(value);
  if (!Number.isFinite(n)) {
    return null;
  }
  return clampFilterValue(n, min, max);
}

/**
 * Sanitize a language code list, or fall back to the default empty list.
 */
function sanitizeLanguages(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((code) => typeof code === "string" && code.length > 0);
}

/**
 * Soft sanitize for persist hydrate before bounds are known.
 */
export function sanitizeFiltersSoft(input) {
  if (input == null || typeof input !== "object" || Array.isArray(input)) {
    return emptyFiltersSoft();
  }
  const voteRaw = input.voteCount;
  let voteCount = null;
  if (voteRaw != null && voteRaw !== "" && Number.isFinite(Number(voteRaw))) {
    voteCount = Number(voteRaw);
  }
  const minRaw = input.minRuntime;
  const maxRaw = input.maxRuntime;
  let minRuntime = null;
  let maxRuntime = null;
  if (minRaw != null && minRaw !== "" && Number.isFinite(Number(minRaw))) {
    minRuntime = Number(minRaw);
  }
  if (maxRaw != null && maxRaw !== "" && Number.isFinite(Number(maxRaw))) {
    maxRuntime = Number(maxRaw);
  }
  return {
    genreFilter: sanitizeTriState(input.genreFilter),
    languageFilter: sanitizeTriState(input.languageFilter),
    yearRange: Array.isArray(input.yearRange) ? [...input.yearRange] : null,
    rating: Array.isArray(input.rating) ? [...input.rating] : null,
    languages: sanitizeLanguages(input.languages),
    adult: typeof input.adult === "boolean" ? input.adult : false,
    voteCount,
    minRuntime,
    maxRuntime,
  };
}

/**
 * Per-field sanitize of persisted or UI filter state. Needs backend bounds.
 */
export function sanitizeFilters(input, bounds) {
  if (bounds == null) {
    return sanitizeFiltersSoft(input);
  }
  const defaults = emptyFilters(bounds);
  if (input == null || typeof input !== "object" || Array.isArray(input)) {
    return defaults;
  }
  let minRuntime = sanitizeNullableNumber(
    input.minRuntime,
    bounds.runtime.min,
    bounds.runtime.max,
  );
  let maxRuntime = sanitizeNullableNumber(
    input.maxRuntime,
    bounds.runtime.min,
    bounds.runtime.max,
  );
  if (minRuntime != null && maxRuntime != null && minRuntime > maxRuntime) {
    const swap = minRuntime;
    minRuntime = maxRuntime;
    maxRuntime = swap;
  }
  return {
    genreFilter: sanitizeTriState(input.genreFilter),
    languageFilter: sanitizeTriState(input.languageFilter),
    yearRange: sanitizeRange(
      input.yearRange,
      defaults.yearRange,
      bounds.yearRange.min,
      bounds.yearRange.max,
    ),
    rating: sanitizeRange(
      input.rating,
      defaults.rating,
      bounds.rating.min,
      bounds.rating.max,
    ),
    languages: sanitizeLanguages(input.languages),
    adult: typeof input.adult === "boolean" ? input.adult : defaults.adult,
    voteCount: sanitizeNullableNumber(
      input.voteCount,
      bounds.voteCount.min,
      bounds.voteCount.max,
    ),
    minRuntime,
    maxRuntime,
  };
}

/**
 * Merge page preset filter fields under user overrides. Null user fields use preset.
 */
export function resolveEffectiveFilters(userFilters, pageKey, mediaType, bounds) {
  const clean = sanitizeFilters(userFilters, bounds);
  const preset = getPresetFilters(pageKey, mediaType);
  return {
    ...clean,
    voteCount: clean.voteCount ?? preset.voteCount ?? null,
    minRuntime: clean.minRuntime ?? preset.minRuntime ?? null,
    maxRuntime: clean.maxRuntime ?? preset.maxRuntime ?? null,
  };
}

/**
 * True when user filter state matches the empty (preset) shape for this bounds.
 */
export function filtersMatchPreset(filters, bounds) {
  if (bounds == null) {
    return false;
  }
  const clean = sanitizeFilters(filters, bounds);
  const empty = emptyFilters(bounds);
  return (
    clean.genreFilter.include.length === 0 &&
    clean.genreFilter.exclude.length === 0 &&
    clean.languageFilter.include.length === 0 &&
    clean.languageFilter.exclude.length === 0 &&
    clean.yearRange[0] === empty.yearRange[0] &&
    clean.yearRange[1] === empty.yearRange[1] &&
    clean.rating[0] === empty.rating[0] &&
    clean.rating[1] === empty.rating[1] &&
    clean.languages.length === 0 &&
    clean.adult === empty.adult &&
    clean.voteCount === null &&
    clean.minRuntime === null &&
    clean.maxRuntime === null
  );
}

/**
 * True when this route sends filters.voteCount to a discover endpoint.
 */
export function showsVoteCountFilter(pathname) {
  if (pathname == null || pathname === "") {
    return false;
  }
  const path = pathname.replace(/\/+$/, "") || "/";
  return (
    path === "/" || path === "/new" || path === "/top" || path === "/upcoming"
  );
}

/**
 * True when this route hits /discover and should show the runtime slider.
 */
export function showsRuntimeFilter(pathname) {
  return showsVoteCountFilter(pathname);
}

/**
 * Read persist JSON. SyntaxError means empty storage, other errors rethrow.
 */
export function readPersistJson(raw) {
  if (raw == null || raw === "") {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    if (error instanceof SyntaxError) {
      return null;
    }
    throw error;
  }
}

/**
 * Migrate persisted filter fields toward null-default vote and runtime shape.
 */
export function migrateFilters(filters, fromVersion) {
  if (!filters || typeof filters !== "object") {
    return filters;
  }
  const next = { ...filters };
  if (fromVersion < 4) {
    if (next.voteCount === 500) {
      next.voteCount = null;
    } else if (
      next.voteCount != null &&
      next.voteCount !== "" &&
      !Number.isFinite(Number(next.voteCount))
    ) {
      next.voteCount = null;
    }
    delete next.duration;
    if (next.minRuntime === undefined) {
      next.minRuntime = null;
    }
    if (next.maxRuntime === undefined) {
      next.maxRuntime = null;
    }
  }
  return next;
}
