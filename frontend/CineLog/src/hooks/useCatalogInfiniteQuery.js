import { useMemo } from "react";
import { useInfiniteQuery, keepPreviousData } from "@tanstack/react-query";
import api from "../api/axiosInstance";
import { buildCatalogParams } from "../lib/catalogParams";
import {
  applyTriStateFilter,
  filterKey,
  isTriStateActive,
  toLanguageValues,
} from "../lib/triStateFilter";
import { normalizeMediaItem } from "../utils/media";
import useAdultPolicy from "./useAdultPolicy";
import useFillVisibleItems from "./useFillVisibleItems";

/**
 * TMDB discover page size. Backend DiscoverFetcher stops on empty or short pages.
 */
export const TMDB_PAGE_SIZE = 20;

/**
 * Filters object from query key (last plain object in the prefix).
 */
function resolveFilters(queryKeyPrefix) {
  for (let i = queryKeyPrefix.length - 1; i >= 0; i -= 1) {
    const part = queryKeyPrefix[i];
    if (part != null && typeof part === "object" && !Array.isArray(part)) {
      return part;
    }
  }
  return {};
}

/**
 * movie/tv hint from catalog query key or endpoint path.
 */
function resolveMediaTypeHint(queryKeyPrefix, endpointPath) {
  for (const part of queryKeyPrefix) {
    if (part === "movie" || part === "tv") {
      return part;
    }
  }
  const match = String(endpointPath ?? "").match(/\/(movie|tv)(?:\/|$)/);
  return match ? match[1] : undefined;
}

/**
 * Normalize discover (bare array) or search (paged object) into one page shape.
 */
function normalizePage(data, mediaTypeHint) {
  let items = [];
  let totalPages = null;

  if (Array.isArray(data)) {
    items = data;
  } else if (
    data != null &&
    typeof data === "object" &&
    Array.isArray(data.results)
  ) {
    items = data.results;
    totalPages =
      typeof data.totalPages === "number" ? data.totalPages : null;
  }

  return {
    items: items.map((item) =>
      normalizeMediaItem(
        mediaTypeHint != null ? { ...item, mediaType: mediaTypeHint } : item,
      ),
    ),
    totalPages,
  };
}

/**
 * Flatten infinite-query pages and keep first occurrence of each id.
 */
function flattenDedupedById(pages) {
  const seen = new Set();
  const out = [];
  for (const page of pages ?? []) {
    const items = Array.isArray(page?.items) ? page.items : [];
    for (const item of items) {
      if (item == null || seen.has(item.id)) {
        continue;
      }
      seen.add(item.id);
      out.push(item);
    }
  }
  return out;
}

/**
 * Infinite catalog browse via React Query. Dedupes by id across all pages once.
 * Optional clientLanguageFilter applies L1 language tri-state on loaded items
 * and fills up to 5 extra pages when the visible list is short.
 */
export default function useCatalogInfiniteQuery(
  queryKeyPrefix,
  endpointPath,
  extraParams = {},
  options = {},
) {
  const { includeAdult } = useAdultPolicy();
  const filters = { ...resolveFilters(queryKeyPrefix), adult: includeAdult };
  const mediaTypeHint = resolveMediaTypeHint(queryKeyPrefix, endpointPath);
  const clientLanguageFilter = options.clientLanguageFilter ?? null;
  const languageActive = isTriStateActive(clientLanguageFilter);

  const query = useInfiniteQuery({
    queryKey: [...queryKeyPrefix, { includeAdult }],
    initialPageParam: 1,
    enabled: options.enabled ?? true,
    placeholderData: keepPreviousData,
    queryFn: async ({ pageParam }) => {
      const res = await api.get(endpointPath, {
        params: buildCatalogParams(pageParam, filters, extraParams),
      });
      return normalizePage(res.data, mediaTypeHint);
    },
    getNextPageParam: (lastPage, allPages) => {
      const items = lastPage?.items ?? [];
      if (!items.length) {
        return null;
      }
      if (typeof lastPage.totalPages === "number") {
        return allPages.length < lastPage.totalPages
          ? allPages.length + 1
          : null;
      }
      if (items.length < TMDB_PAGE_SIZE) {
        return null;
      }
      return allPages.length + 1;
    },
  });

  const rawResults = flattenDedupedById(query.data?.pages);
  const results = useMemo(() => {
    if (!languageActive) {
      return rawResults;
    }
    return applyTriStateFilter(
      rawResults,
      clientLanguageFilter,
      toLanguageValues,
      "any",
    );
  }, [rawResults, languageActive, clientLanguageFilter]);

  const fetchNextPage = query.fetchNextPage;
  const hasNextPage = Boolean(query.hasNextPage);
  const isFetching = query.isFetching;
  const isFetchingNextPage = query.isFetchingNextPage;

  useFillVisibleItems({
    visibleCount: results.length,
    hasNextPage,
    isFetching: isFetching || isFetchingNextPage,
    fetchNextPage,
    minVisible: TMDB_PAGE_SIZE,
    maxPages: 5,
    filterKey: languageActive ? filterKey(clientLanguageFilter) : null,
    enabled: Boolean(options.fillOnLanguageFilter && languageActive),
  });

  return {
    results,
    fetchNextPage,
    hasNextPage,
    pagesLoaded: query.data?.pages?.length ?? 0,
    isFetching,
    isFetchingNextPage,
    isLoading: query.isLoading,
    isPlaceholderData: query.isPlaceholderData,
  };
}
