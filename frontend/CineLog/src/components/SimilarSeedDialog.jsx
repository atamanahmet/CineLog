import { useCallback, useEffect, useRef, useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import SeedPosterCard, { SeedPosterSkeleton } from "./SeedPosterCard";
import { searchTitles } from "../api/similarApi";
import useDebouncedValue from "../hooks/useDebouncedValue";
import useInfiniteScrollTrigger, {
  shouldFetchNextPage,
} from "../hooks/useInfiniteScroll";
import { PAGE_MEMORY_TTL_MS } from "../lib/pageMemory";
import { flattenSeedSearchPages } from "../lib/seedSearchResults";
import { MAX_SEEDS, useSimilarStore } from "../stores/similarStore";
import { mediaReleaseYear } from "../utils/media";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";

const SEARCH_DEBOUNCE_MS = 300;
const SKELETON_COUNT = 10;
const NEXT_PAGE_SKELETON_COUNT = 5;
const SEED_SEARCH_MAX_PAGES = 5;
const SEED_SEARCH_MIN_VOTES = { movie: 100, tv: 50 };
const RESULT_GRID_CLASS =
  "grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5";

/**
 * Calendar year number from a media item, or null.
 */
function itemYear(item) {
  const year = mediaReleaseYear(item);
  return year === "Unknown" ? null : year;
}

/**
 * True when this result is already a seed.
 */
function isSelected(seeds, item) {
  return seeds.some(
    (seed) => seed.tmdbId === item.id && seed.mediaType === item.mediaType,
  );
}

/**
 * Next page number from TMDB totalPages and the 5 page cap only.
 */
function getSeedSearchNextPage(lastPage, allPages) {
  if (allPages.length >= SEED_SEARCH_MAX_PAGES) {
    return undefined;
  }
  if (
    typeof lastPage?.totalPages === "number" &&
    allPages.length >= lastPage.totalPages
  ) {
    return undefined;
  }
  return allPages.length + 1;
}

/**
 * Modal to search and pick up to MAX_SEEDS titles as similar seeds.
 */
export default function SimilarSeedDialog({ open, onOpenChange }) {
  const seeds = useSimilarStore((s) => s.seeds);
  const addSeed = useSimilarStore((s) => s.addSeed);
  const removeSeed = useSimilarStore((s) => s.removeSeed);
  const [query, setQuery] = useState("");
  const debounced = useDebouncedValue(query.trim(), SEARCH_DEBOUNCE_MS);
  const canSearch = debounced.length >= 2;
  const atCap = seeds.length >= MAX_SEEDS;

  const scrollRootRef = useRef(null);
  const movieSentinelRef = useRef(null);
  const tvSentinelRef = useRef(null);

  const movieQuery = useInfiniteQuery({
    queryKey: [
      "similar-search",
      "MOVIE",
      debounced,
      SEED_SEARCH_MIN_VOTES.movie,
    ],
    queryFn: ({ pageParam }) =>
      searchTitles(
        "MOVIE",
        debounced,
        pageParam,
        SEED_SEARCH_MIN_VOTES.movie,
      ),
    initialPageParam: 1,
    getNextPageParam: getSeedSearchNextPage,
    enabled: open && canSearch,
    staleTime: 60_000,
    gcTime: PAGE_MEMORY_TTL_MS,
  });

  const tvQuery = useInfiniteQuery({
    queryKey: ["similar-search", "TV", debounced, SEED_SEARCH_MIN_VOTES.tv],
    queryFn: ({ pageParam }) =>
      searchTitles("TV", debounced, pageParam, SEED_SEARCH_MIN_VOTES.tv),
    initialPageParam: 1,
    getNextPageParam: getSeedSearchNextPage,
    enabled: open && canSearch,
    staleTime: 60_000,
    gcTime: PAGE_MEMORY_TTL_MS,
  });

  const movieHasNextPage = Boolean(movieQuery.hasNextPage);
  const movieFetchingNext = movieQuery.isFetchingNextPage;
  const moviePagesLoaded = movieQuery.data?.pages?.length ?? 0;
  const fetchNextMovies = movieQuery.fetchNextPage;

  const tvHasNextPage = Boolean(tvQuery.hasNextPage);
  const tvFetchingNext = tvQuery.isFetchingNextPage;
  const tvPagesLoaded = tvQuery.data?.pages?.length ?? 0;
  const fetchNextTv = tvQuery.fetchNextPage;

  const tryFetchMovies = useCallback(() => {
    if (
      shouldFetchNextPage({
        isIntersecting: true,
        hasNextPage: movieHasNextPage,
        isFetchingNextPage: movieFetchingNext,
        pagesLoaded: moviePagesLoaded,
        maxPages: SEED_SEARCH_MAX_PAGES,
      })
    ) {
      fetchNextMovies();
    }
  }, [movieHasNextPage, movieFetchingNext, moviePagesLoaded, fetchNextMovies]);

  const tryFetchTv = useCallback(() => {
    if (
      shouldFetchNextPage({
        isIntersecting: true,
        hasNextPage: tvHasNextPage,
        isFetchingNextPage: tvFetchingNext,
        pagesLoaded: tvPagesLoaded,
        maxPages: SEED_SEARCH_MAX_PAGES,
      })
    ) {
      fetchNextTv();
    }
  }, [tvHasNextPage, tvFetchingNext, tvPagesLoaded, fetchNextTv]);

  const movies = flattenSeedSearchPages(movieQuery.data?.pages);
  const tv = flattenSeedSearchPages(tvQuery.data?.pages);

  useInfiniteScrollTrigger(
    movieSentinelRef,
    scrollRootRef,
    tryFetchMovies,
    movies.length > 0,
  );
  useInfiniteScrollTrigger(
    tvSentinelRef,
    scrollRootRef,
    tryFetchTv,
    tv.length > 0,
  );

  useEffect(() => {
    if (!open) {
      setQuery("");
    }
  }, [open]);

  useEffect(() => {
    if (!open || !canSearch || movieQuery.data == null) {
      return;
    }
    if (movies.length > 0) {
      return;
    }
    if (
      shouldFetchNextPage({
        isIntersecting: true,
        hasNextPage: movieHasNextPage,
        isFetchingNextPage: movieFetchingNext,
        pagesLoaded: moviePagesLoaded,
        maxPages: SEED_SEARCH_MAX_PAGES,
      })
    ) {
      fetchNextMovies();
    }
  }, [
    open,
    canSearch,
    movieQuery.data,
    movies.length,
    movieHasNextPage,
    movieFetchingNext,
    moviePagesLoaded,
    fetchNextMovies,
  ]);

  useEffect(() => {
    if (!open || !canSearch || tvQuery.data == null) {
      return;
    }
    if (tv.length > 0) {
      return;
    }
    if (
      shouldFetchNextPage({
        isIntersecting: true,
        hasNextPage: tvHasNextPage,
        isFetchingNextPage: tvFetchingNext,
        pagesLoaded: tvPagesLoaded,
        maxPages: SEED_SEARCH_MAX_PAGES,
      })
    ) {
      fetchNextTv();
    }
  }, [
    open,
    canSearch,
    tvQuery.data,
    tv.length,
    tvHasNextPage,
    tvFetchingNext,
    tvPagesLoaded,
    fetchNextTv,
  ]);

  function handleToggle(item) {
    if (isSelected(seeds, item)) {
      removeSeed(item.id, item.mediaType);
      return;
    }
    addSeed({
      tmdbId: item.id,
      mediaType: item.mediaType,
      title: item.title,
      posterPath: item.posterPath,
      year: itemYear(item),
    });
  }

  const loading =
    canSearch &&
    (movieQuery.isPending || tvQuery.isPending) &&
    movieQuery.data == null &&
    tvQuery.data == null;
  const errored =
    canSearch &&
    !loading &&
    movies.length === 0 &&
    tv.length === 0 &&
    (movieQuery.isError || tvQuery.isError);
  const movieCanLoadMore =
    movieHasNextPage && moviePagesLoaded < SEED_SEARCH_MAX_PAGES;
  const tvCanLoadMore = tvHasNextPage && tvPagesLoaded < SEED_SEARCH_MAX_PAGES;
  const fillingEmpty =
    canSearch &&
    !loading &&
    !errored &&
    movies.length === 0 &&
    tv.length === 0 &&
    (movieCanLoadMore ||
      tvCanLoadMore ||
      movieFetchingNext ||
      tvFetchingNext);
  const empty =
    canSearch &&
    !loading &&
    !errored &&
    !fillingEmpty &&
    movies.length === 0 &&
    tv.length === 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[90vh] w-[calc(100%-1.5rem)] max-w-3xl flex-col gap-3 overflow-hidden bg-card p-4 text-card-foreground sm:p-6">
        <DialogHeader>
          <DialogTitle>Add titles</DialogTitle>
        </DialogHeader>

        <Input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search movies or TV..."
          aria-label="Search titles"
          autoFocus
        />

        {atCap && (
          <p className="text-sm text-muted-foreground">
            You can pick up to 10 titles.
          </p>
        )}

        <div ref={scrollRootRef} className="min-h-0 flex-1 overflow-y-auto pr-1">
          {!canSearch && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Type at least 2 characters to search.
            </p>
          )}
          {loading && (
            <div className="space-y-6" aria-busy="true">
              <span className="sr-only">Loading</span>
              <div>
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Movies
                </p>
                <div className={RESULT_GRID_CLASS}>
                  {Array.from({ length: SKELETON_COUNT }, (_, i) => (
                    <SeedPosterSkeleton key={`movie-sk-${i}`} />
                  ))}
                </div>
              </div>
              <div>
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  TV
                </p>
                <div className={RESULT_GRID_CLASS}>
                  {Array.from({ length: SKELETON_COUNT }, (_, i) => (
                    <SeedPosterSkeleton key={`tv-sk-${i}`} />
                  ))}
                </div>
              </div>
            </div>
          )}
          {errored && !loading && (
            <p className="py-4 text-center text-sm text-foreground">
              Search failed. Try again.
            </p>
          )}
          {fillingEmpty && (
            <div className={RESULT_GRID_CLASS} aria-busy="true">
              <span className="sr-only">Loading</span>
              {Array.from({ length: NEXT_PAGE_SKELETON_COUNT }, (_, i) => (
                <SeedPosterSkeleton key={`fill-sk-${i}`} />
              ))}
            </div>
          )}
          {empty && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              No titles found.
            </p>
          )}
          {!loading && movies.length > 0 && (
            <div className="mb-6">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Movies
              </p>
              <div className={RESULT_GRID_CLASS}>
                {movies.map((item) => {
                  const selected = isSelected(seeds, item);
                  return (
                    <SeedPosterCard
                      key={`MOVIE-${item.id}`}
                      item={item}
                      selected={selected}
                      disabled={atCap && !selected}
                      onToggle={handleToggle}
                    />
                  );
                })}
                {movieQuery.isFetchingNextPage &&
                  Array.from({ length: NEXT_PAGE_SKELETON_COUNT }, (_, i) => (
                    <SeedPosterSkeleton key={`movie-next-sk-${i}`} />
                  ))}
              </div>
              <div ref={movieSentinelRef} aria-hidden="true" className="h-1" />
            </div>
          )}
          {!loading && tv.length > 0 && (
            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                TV
              </p>
              <div className={RESULT_GRID_CLASS}>
                {tv.map((item) => {
                  const selected = isSelected(seeds, item);
                  return (
                    <SeedPosterCard
                      key={`TV-${item.id}`}
                      item={item}
                      selected={selected}
                      disabled={atCap && !selected}
                      onToggle={handleToggle}
                    />
                  );
                })}
                {tvQuery.isFetchingNextPage &&
                  Array.from({ length: NEXT_PAGE_SKELETON_COUNT }, (_, i) => (
                    <SeedPosterSkeleton key={`tv-next-sk-${i}`} />
                  ))}
              </div>
              <div ref={tvSentinelRef} aria-hidden="true" className="h-1" />
            </div>
          )}
        </div>

        <DialogFooter className="shrink-0 gap-2 sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            {seeds.length} of {MAX_SEEDS} selected
          </p>
          <Button type="button" onClick={() => onOpenChange(false)}>
            Done
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
