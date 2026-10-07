import { useMemo } from "react";
import { useLocation } from "react-router";
import useDebouncedValue from "./useDebouncedValue";
import usePageFilters from "./usePageFilters";
import { filterScopeKey } from "../lib/filterScope";
import { languageServerHint } from "../lib/languageServerHint";
import { emptyTriState, filterKey, isTriStateActive } from "../lib/triStateFilter";
import { pageKeyFromPath } from "../lib/pagePresets";
import { useUserPrefsStore } from "../stores/userPrefsStore";

const GENRE_DEBOUNCE_MS = 600;

/**
 * Resolve effective catalog filters and sort for the current page.
 * Genre filter is debounced for the query key; chips stay instant via store.
 * Language: query key only carries the single-include TMDB hint.
 */
export default function useEffectiveCatalogFilters(pageKey) {
  const location = useLocation();
  const key = pageKey ?? pageKeyFromPath(location.pathname);
  const scopeKey = filterScopeKey(location.pathname);
  const mediaType = useUserPrefsStore((s) => s.mediaType);
  const sortByPage = useUserPrefsStore((s) => s.sortByPage);
  const getEffectiveSort = useUserPrefsStore((s) => s.getEffectiveSort);

  const { bounds, effectiveFilters, filters, setFilters } = usePageFilters(
    scopeKey,
    mediaType,
    key,
  );

  const genreFilter = effectiveFilters.genreFilter ?? emptyTriState();
  const debouncedGenreFilter = useDebouncedValue(genreFilter, GENRE_DEBOUNCE_MS);
  const queryGenreFilter = isTriStateActive(genreFilter)
    ? debouncedGenreFilter
    : emptyTriState();

  const languageFilter = effectiveFilters.languageFilter ?? emptyTriState();
  const languageHint = languageServerHint(languageFilter);

  const queryFilters = useMemo(() => {
    const { languageFilter: _clientLanguage, ...rest } = effectiveFilters;
    return {
      ...rest,
      genreFilter: queryGenreFilter,
      genreKey: filterKey(queryGenreFilter),
      languages: languageHint ? [languageHint] : [],
    };
  }, [effectiveFilters, queryGenreFilter, languageHint]);

  const sort = useMemo(() => {
    void sortByPage;
    return getEffectiveSort(key);
  }, [getEffectiveSort, key, sortByPage]);

  return {
    bounds,
    effectiveFilters,
    queryFilters,
    filters,
    setFilters,
    sort,
    mediaType,
    pageKey: key,
  };
}
