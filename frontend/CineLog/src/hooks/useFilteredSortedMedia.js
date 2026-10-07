import { useMemo } from "react";
import usePageFilters from "./usePageFilters";
import { emptyFilters } from "../lib/sanitizeFilters";
import {
  filterMediaItems,
  mergeByScore,
  sortMediaItems,
} from "../utils/media";

/**
 * True when any visible filter key is away from its empty default.
 */
export function isMediaFilterNarrowed(filters, activeKeys, bounds) {
  if (filters == null || bounds == null || !Array.isArray(activeKeys)) {
    return false;
  }
  const empty = emptyFilters(bounds);
  for (const key of activeKeys) {
    if (key === "languages" && (filters.languages?.length ?? 0) > 0) {
      return true;
    }
    if (key === "adult" && filters.adult === true) {
      return true;
    }
    if (
      key === "yearRange" &&
      Array.isArray(filters.yearRange) &&
      (filters.yearRange[0] !== empty.yearRange[0] ||
        filters.yearRange[1] !== empty.yearRange[1])
    ) {
      return true;
    }
    if (
      key === "rating" &&
      Array.isArray(filters.rating) &&
      (filters.rating[0] !== empty.rating[0] ||
        filters.rating[1] !== empty.rating[1])
    ) {
      return true;
    }
    if (key === "voteCount" && filters.voteCount != null) {
      return true;
    }
    if (
      key === "runtime" &&
      (filters.minRuntime != null || filters.maxRuntime != null)
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Merge (when Mixed), filter by activeKeys, then sort. Pure helper for tests and the hook.
 */
export function deriveFilteredSortedMedia({
  movies,
  tv,
  view,
  sortKey,
  filters,
  activeKeys,
  bounds,
}) {
  let base = null;
  if (view === "all") {
    if (movies == null && tv == null) {
      return { items: null, total: 0, shown: 0, narrowed: false };
    }
    base = mergeByScore(movies ?? [], tv ?? []);
  } else if (view === "tv") {
    if (tv == null) {
      return { items: null, total: 0, shown: 0, narrowed: false };
    }
    base = tv;
  } else {
    if (movies == null) {
      return { items: null, total: 0, shown: 0, narrowed: false };
    }
    base = movies;
  }

  const total = base.length;
  const filtered = filterMediaItems(base, filters, activeKeys, bounds);
  const items = sortMediaItems(filtered, sortKey);
  const narrowed = isMediaFilterNarrowed(filters, activeKeys, bounds);
  return { items, total, shown: items.length, narrowed };
}

/**
 * Filtered and sorted media for Recommendation and Find similar tabs.
 */
export default function useFilteredSortedMedia({
  movies,
  tv,
  view,
  sortKey,
  activeKeys,
  bounds,
  scopeKey,
  mediaType,
}) {
  const { filters } = usePageFilters(scopeKey, mediaType ?? view);

  return useMemo(
    () =>
      deriveFilteredSortedMedia({
        movies,
        tv,
        view,
        sortKey,
        filters,
        activeKeys,
        bounds,
      }),
    [movies, tv, view, sortKey, filters, activeKeys, bounds],
  );
}
