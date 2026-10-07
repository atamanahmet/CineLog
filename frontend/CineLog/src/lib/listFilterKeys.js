import { GENRE_INCLUDE_MODE, toCanonicalGenreIds } from "./genreCanonical";
import {
  applyTriStateFilter,
  emptyTriState,
  isTriStateActive,
  toLanguageValues,
} from "./triStateFilter";
import { filterMediaItems } from "../utils/media";
import { emptyFilters } from "./sanitizeFilters";

/** Client filter keys that match fields on list / card items (not tri-state chips). */
export const LIST_FILTER_ACTIVE_KEYS = [
  "yearRange",
  "rating",
  "adult",
];

/**
 * Filter a flat list: year/rating/adult then genre + language tri-state.
 */
export function filterListItems(items, filters, bounds) {
  const base = filterMediaItems(
    items ?? [],
    filters,
    LIST_FILTER_ACTIVE_KEYS,
    bounds,
  );
  const afterGenre = applyTriStateFilter(
    base,
    filters?.genreFilter,
    toCanonicalGenreIds,
    GENRE_INCLUDE_MODE,
  );
  return applyTriStateFilter(
    afterGenre,
    filters?.languageFilter,
    toLanguageValues,
    "any",
  );
}

/**
 * True when list filters are away from empty defaults (incl. tri-state chips).
 */
export function isListFilterNarrowed(filters, bounds) {
  if (filters == null || bounds == null) {
    return false;
  }
  if (isTriStateActive(filters.genreFilter ?? emptyTriState())) {
    return true;
  }
  if (isTriStateActive(filters.languageFilter ?? emptyTriState())) {
    return true;
  }
  const empty = emptyFilters(bounds);
  if (
    Array.isArray(filters.yearRange) &&
    (filters.yearRange[0] !== empty.yearRange[0] ||
      filters.yearRange[1] !== empty.yearRange[1])
  ) {
    return true;
  }
  if (
    Array.isArray(filters.rating) &&
    (filters.rating[0] !== empty.rating[0] ||
      filters.rating[1] !== empty.rating[1])
  ) {
    return true;
  }
  if (filters.adult === true) {
    return true;
  }
  return false;
}

/**
 * True when genre tri-state would change the list.
 */
export function genreFilterActs(filter) {
  return isTriStateActive(filter ?? emptyTriState());
}
