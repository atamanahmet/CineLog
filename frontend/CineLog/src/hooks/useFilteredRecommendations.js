import { useQuery, keepPreviousData } from "@tanstack/react-query";
import api from "../api/axiosInstance";
import { PAGE_MEMORY_TTL_MS } from "../lib/pageMemory";
import { isTriStateActive, sortedValuesKey } from "../lib/triStateFilter";
import { mergeByScore, normalizeMediaItem } from "../utils/media";

/** Same TTL as page filter memory (3 minutes). */
export const RECOMMENDATION_FILTERED_CACHE_MS = PAGE_MEMORY_TTL_MS;

/**
 * React Query key for one filtered recommendation media type.
 */
export function recommendationFilteredKey(mediaType, includeKey, excludeKey) {
  return ["recommendationFiltered", mediaType, includeKey, excludeKey];
}

/**
 * Fetch one media type with genre filters. Comma-joined query params.
 */
export async function fetchFilteredRecommendation(mediaType, genreFilter) {
  const params = { mediaType };
  const include = genreFilter?.include ?? [];
  const exclude = genreFilter?.exclude ?? [];
  if (include.length > 0) {
    params.genreInclude = include.join(",");
  }
  if (exclude.length > 0) {
    params.genreExclude = exclude.join(",");
  }
  const res = await api.get("/user/recommendation", { params });
  return normalizeList(res.data);
}

function normalizeList(data) {
  if (!Array.isArray(data)) {
    return [];
  }
  return data.map(normalizeMediaItem);
}

/**
 * Filtered recommendation queries. Empty genre filter disables fetching.
 */
export default function useFilteredRecommendations(view, genreFilter, enabled) {
  const active = enabled && isTriStateActive(genreFilter);
  const includeKey = sortedValuesKey(genreFilter?.include);
  const excludeKey = sortedValuesKey(genreFilter?.exclude);
  const apiMovie = "MOVIE";
  const apiTv = "TV";

  const movieQuery = useQuery({
    queryKey: recommendationFilteredKey(apiMovie, includeKey, excludeKey),
    queryFn: () => fetchFilteredRecommendation(apiMovie, genreFilter),
    enabled: active && (view === "movie" || view === "all"),
    staleTime: RECOMMENDATION_FILTERED_CACHE_MS,
    gcTime: RECOMMENDATION_FILTERED_CACHE_MS,
    placeholderData: keepPreviousData,
  });

  const tvQuery = useQuery({
    queryKey: recommendationFilteredKey(apiTv, includeKey, excludeKey),
    queryFn: () => fetchFilteredRecommendation(apiTv, genreFilter),
    enabled: active && (view === "tv" || view === "all"),
    staleTime: RECOMMENDATION_FILTERED_CACHE_MS,
    gcTime: RECOMMENDATION_FILTERED_CACHE_MS,
    placeholderData: keepPreviousData,
  });

  if (!active) {
    return {
      active: false,
      movies: null,
      tv: null,
      isLoading: false,
      isFetching: false,
      isError: false,
      movieQuery: null,
      tvQuery: null,
    };
  }

  if (view === "movie") {
    return {
      active: true,
      movies: movieQuery.data ?? null,
      tv: null,
      isLoading: movieQuery.isLoading && movieQuery.data == null,
      isFetching: movieQuery.isFetching,
      isError: movieQuery.isError && movieQuery.data == null,
      movieQuery,
      tvQuery: null,
    };
  }

  if (view === "tv") {
    return {
      active: true,
      movies: null,
      tv: tvQuery.data ?? null,
      isLoading: tvQuery.isLoading && tvQuery.data == null,
      isFetching: tvQuery.isFetching,
      isError: tvQuery.isError && tvQuery.data == null,
      movieQuery: null,
      tvQuery,
    };
  }

  const movieReady = movieQuery.isSuccess || movieQuery.isError;
  const tvReady = tvQuery.isSuccess || tvQuery.isError;
  const bothMissing = movieQuery.data == null && tvQuery.data == null;
  const isLoading =
    bothMissing &&
    ((movieQuery.isLoading && !movieReady) ||
      (tvQuery.isLoading && !tvReady) ||
      movieQuery.isFetching ||
      tvQuery.isFetching);

  return {
    active: true,
    movies: movieQuery.data ?? null,
    tv: tvQuery.data ?? null,
    isLoading,
    isFetching: movieQuery.isFetching || tvQuery.isFetching,
    isError: bothMissing && (movieQuery.isError || tvQuery.isError),
    movieQuery,
    tvQuery,
    merged:
      movieQuery.data != null || tvQuery.data != null
        ? mergeByScore(movieQuery.data ?? [], tvQuery.data ?? [])
        : null,
  };
}
