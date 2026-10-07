import { useCallback, useMemo } from "react";
import useDiscoverDefaults from "./useDiscoverDefaults";
import {
  emptyFiltersSoft,
  filtersAfterReset,
  resolveEffectiveFilters,
  sanitizeFilters,
} from "../lib/sanitizeFilters";
import { usePageFiltersStore } from "../stores/pageFiltersStore";

/**
 * Read and write filters for one page scope. No lifecycle.
 */
export default function usePageFilters(scopeKey, mediaType, pageKey) {
  const { data: bounds } = useDiscoverDefaults();
  const entries = usePageFiltersStore((s) => s.entries);
  const storeGetFilters = usePageFiltersStore((s) => s.getFilters);
  const storeSetFilters = usePageFiltersStore((s) => s.setFilters);
  const storeResetFilters = usePageFiltersStore((s) => s.resetFilters);
  const presetKey = pageKey ?? scopeKey;

  const storedFilters = useMemo(() => {
    void entries;
    if (scopeKey == null) {
      return null;
    }
    return storeGetFilters(scopeKey, mediaType);
  }, [entries, mediaType, scopeKey, storeGetFilters]);

  const filters = useMemo(() => {
    if (storedFilters != null) {
      return bounds ? sanitizeFilters(storedFilters, bounds) : storedFilters;
    }
    return bounds ? filtersAfterReset(bounds) : emptyFiltersSoft();
  }, [bounds, storedFilters]);

  const effectiveFilters = useMemo(() => {
    if (!bounds || presetKey == null) {
      return filters;
    }
    return resolveEffectiveFilters(
      storedFilters ?? filtersAfterReset(bounds),
      presetKey,
      mediaType,
      bounds,
    );
  }, [bounds, filters, mediaType, presetKey, storedFilters]);

  const setFilters = useCallback(
    (next, nextBounds) => {
      if (scopeKey == null) {
        return;
      }
      storeSetFilters(
        scopeKey,
        sanitizeFilters(next, nextBounds ?? bounds),
        mediaType,
      );
    },
    [bounds, mediaType, scopeKey, storeSetFilters],
  );

  const resetFilters = useCallback(() => {
    if (scopeKey == null) {
      return;
    }
    storeResetFilters(scopeKey);
  }, [scopeKey, storeResetFilters]);

  return {
    bounds,
    filters,
    effectiveFilters,
    setFilters,
    resetFilters,
    scopeKey,
  };
}
