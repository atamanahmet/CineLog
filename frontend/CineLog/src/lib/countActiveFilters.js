import { countActive } from "./triStateFilter";

/**
 * Count filters that differ from defaults. Scope.languages gates language chips.
 */
export function countActiveFilters(filters, defaults, scope) {
  if (filters == null || defaults == null) {
    return 0;
  }

  let count = 0;

  count += countActive(filters.genreFilter);

  if (scope?.languages) {
    count += countActive(filters.languageFilter);
    const currentLanguages = filters.languages ?? [];
    const defaultLanguages = defaults.languages ?? [];
    if (!samePrimitiveList(currentLanguages, defaultLanguages)) {
      count += 1;
    }
  }

  if (scope?.yearRange !== false && rangeDiffers(filters.yearRange, defaults.yearRange)) {
    count += 1;
  }

  if (rangeDiffers(filters.rating, defaults.rating)) {
    count += 1;
  }

  if ((filters.adult ?? false) !== (defaults.adult ?? false)) {
    count += 1;
  }

  if ((filters.voteCount ?? null) !== (defaults.voteCount ?? null)) {
    count += 1;
  }

  if (
    (filters.minRuntime ?? null) !== (defaults.minRuntime ?? null) ||
    (filters.maxRuntime ?? null) !== (defaults.maxRuntime ?? null)
  ) {
    count += 1;
  }

  return count;
}

/**
 * Badge label for an active count. Null when zero. Caps at 9+.
 */
export function formatActiveFilterBadge(count) {
  if (count == null || count <= 0) {
    return null;
  }
  if (count > 9) {
    return "9+";
  }
  return String(count);
}

function rangeDiffers(current, fallback) {
  if (!Array.isArray(current) || current.length !== 2) {
    return false;
  }
  if (!Array.isArray(fallback) || fallback.length !== 2) {
    return true;
  }
  return current[0] !== fallback[0] || current[1] !== fallback[1];
}

function samePrimitiveList(a, b) {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i += 1) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
}
