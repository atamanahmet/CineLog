import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { fetchSimilarRecommendations } from "../api/similarApi";
import { PAGE_MEMORY_TTL_MS } from "../lib/pageMemory";
import { sortedValuesKey } from "../lib/triStateFilter";
import { similarKey } from "../utils/similarKey";
import { mergeByScore } from "../utils/media";

/** Shared staleTime and gcTime for similar result queries. */
export const SIMILAR_CACHE_MS = PAGE_MEMORY_TTL_MS;

/**
 * React Query key for one similar media type plus genre filter keys.
 */
export function similarQueryKey(type, seeds, includeKey = "", excludeKey = "") {
  return ["similar", type, similarKey(seeds), includeKey, excludeKey];
}

/**
 * Fetch similar titles for the active view. Disabled while the seed modal is open.
 */
export function useSimilarRecommendations(seeds, view, modalOpen, genreFilter) {
  const hasSeeds = Array.isArray(seeds) && seeds.length > 0;
  const enabled = hasSeeds && !modalOpen;
  const keyPart = similarKey(seeds);
  const includeKey = sortedValuesKey(genreFilter?.include);
  const excludeKey = sortedValuesKey(genreFilter?.exclude);

  const movieQuery = useQuery({
    queryKey: similarQueryKey("MOVIE", seeds, includeKey, excludeKey),
    queryFn: () =>
      fetchSimilarRecommendations(seeds, "MOVIE", genreFilter),
    enabled: enabled && (view === "movie" || view === "all"),
    staleTime: SIMILAR_CACHE_MS,
    gcTime: SIMILAR_CACHE_MS,
    placeholderData: keepPreviousData,
  });

  const tvQuery = useQuery({
    queryKey: similarQueryKey("TV", seeds, includeKey, excludeKey),
    queryFn: () => fetchSimilarRecommendations(seeds, "TV", genreFilter),
    enabled: enabled && (view === "tv" || view === "all"),
    staleTime: SIMILAR_CACHE_MS,
    gcTime: SIMILAR_CACHE_MS,
    placeholderData: keepPreviousData,
  });

  if (view === "movie") {
    return {
      items: movieQuery.data ?? null,
      isLoading: movieQuery.isLoading && movieQuery.data == null,
      isFetching: movieQuery.isFetching,
      isError: movieQuery.isError,
      error: movieQuery.error,
      refetch: () => movieQuery.refetch(),
      movieQuery,
      tvQuery: null,
      keyPart,
    };
  }

  if (view === "tv") {
    return {
      items: tvQuery.data ?? null,
      isLoading: tvQuery.isLoading && tvQuery.data == null,
      isFetching: tvQuery.isFetching,
      isError: tvQuery.isError,
      error: tvQuery.error,
      refetch: () => tvQuery.refetch(),
      movieQuery: null,
      tvQuery,
      keyPart,
    };
  }

  const movieReady = movieQuery.isSuccess || movieQuery.isError;
  const tvReady = tvQuery.isSuccess || tvQuery.isError;
  const bothMissing = movieQuery.data == null && tvQuery.data == null;
  const isLoading =
    (!movieReady && movieQuery.isFetching) ||
    (!tvReady && tvQuery.isFetching) ||
    (bothMissing && (movieQuery.isLoading || tvQuery.isLoading));

  let items = null;
  if (movieQuery.data != null || tvQuery.data != null) {
    items = mergeByScore(movieQuery.data ?? [], tvQuery.data ?? []);
  }

  return {
    items,
    isLoading: isLoading && bothMissing,
    isFetching: movieQuery.isFetching || tvQuery.isFetching,
    isError: bothMissing && (movieQuery.isError || tvQuery.isError),
    bothSettled: movieReady && tvReady,
    movieQuery,
    tvQuery,
    keyPart,
  };
}
