import { useMemo } from "react";
import CardGrid from "../components/CardGrid";
import CardPlate from "../components/CardPlate";
import { CardGridSkeletonItems } from "../components/CardGridSkeleton";
import ClientFilterEmpty from "../components/ClientFilterEmpty";
import ListControls from "../components/ListControls";
import useCatalogInfiniteQuery, {
  TMDB_PAGE_SIZE,
} from "../hooks/useCatalogInfiniteQuery";
import useEffectiveCatalogFilters from "../hooks/useEffectiveCatalogFilters";
import useFillVisibleItems from "../hooks/useFillVisibleItems";
import useInfiniteScrollTrigger from "../hooks/useInfiniteScrollTrigger";
import {
  GENRE_INCLUDE_MODE,
  toCanonicalGenreIds,
} from "../lib/genreCanonical";
import { getMediaTypeLabel } from "../lib/mediaLabels";
import { buildDiscoverSortOptions } from "../lib/sortOptions";
import {
  applyTriStateFilter,
  emptyTriState,
  filterKey,
  isTriStateActive,
  toLanguageValues,
} from "../lib/triStateFilter";
import { useFiltersStore } from "../stores/filtersStore";

/** Flip to true to enable Search sort dropdown + client-side sort. Single source of truth. */
const SEARCH_SORT_ENABLED = false;

/**
 * Sort already-fetched search results by a TMDB sort_by token (e.g. popularity.desc).
 * Maps tokens onto API list item fields (camelCase).
 */
function sortSearchResults(results, sortBy) {
  if (!sortBy || !Array.isArray(results) || results.length === 0) {
    return results;
  }

  const dot = sortBy.lastIndexOf(".");
  if (dot < 0) {
    return results;
  }
  const field = sortBy.slice(0, dot);
  const direction = sortBy.slice(dot + 1);
  const desc = direction === "desc";

  const getValue = (item) => {
    switch (field) {
      case "popularity":
        return item?.popularity ?? null;
      case "vote_average":
        return item?.voteAverage ?? null;
      case "primary_release_date":
      case "first_air_date":
      case "release_date":
        return item?.releaseDate ?? null;
      case "title":
      case "name":
        return (item?.title ?? item?.originalTitle ?? "").toString().toLowerCase();
      default:
        return null;
    }
  };

  return [...results].sort((a, b) => {
    const av = getValue(a);
    const bv = getValue(b);
    if (av == null && bv == null) {
      return 0;
    }
    if (av == null) {
      return 1;
    }
    if (bv == null) {
      return -1;
    }
    let cmp = 0;
    if (typeof av === "string" || typeof bv === "string") {
      cmp = String(av).localeCompare(String(bv));
    } else {
      cmp = av < bv ? -1 : av > bv ? 1 : 0;
    }
    return desc ? -cmp : cmp;
  });
}

/**
 * Search results with infinite scroll. Genre and language filters are client-side.
 */
export default function SearchPage() {
  const {
    mediaType,
    effectiveFilters,
    filters,
    setFilters,
    bounds,
    sort,
  } = useEffectiveCatalogFilters("search");
  const searchQuery = useFiltersStore((s) => s.searchQuery);
  const hasQuery = Boolean(searchQuery?.trim());

  const serverFilters = useMemo(
    () => ({
      ...effectiveFilters,
      genreFilter: emptyTriState(),
      languageFilter: emptyTriState(),
    }),
    [effectiveFilters],
  );

  const {
    results,
    fetchNextPage,
    hasNextPage,
    isFetching,
    isFetchingNextPage,
    isLoading,
  } = useCatalogInfiniteQuery(
    ["search", mediaType, searchQuery, serverFilters],
    `/${mediaType}/search`,
    { query: searchQuery },
    { enabled: hasQuery },
  );

  useInfiniteScrollTrigger(fetchNextPage, isFetching);

  const genreFilter = filters.genreFilter ?? emptyTriState();
  const languageFilter = filters.languageFilter ?? emptyTriState();
  const clientFilterActive =
    isTriStateActive(genreFilter) || isTriStateActive(languageFilter);
  const clientFilterKey = `${filterKey(genreFilter)}|${filterKey(languageFilter)}`;

  const filteredResults = useMemo(() => {
    const afterGenre = applyTriStateFilter(
      results,
      genreFilter,
      toCanonicalGenreIds,
      GENRE_INCLUDE_MODE,
    );
    return applyTriStateFilter(
      afterGenre,
      languageFilter,
      toLanguageValues,
      "any",
    );
  }, [results, genreFilter, languageFilter]);

  useFillVisibleItems({
    visibleCount: filteredResults.length,
    hasNextPage,
    isFetching: isFetching || isFetchingNextPage,
    fetchNextPage,
    minVisible: TMDB_PAGE_SIZE,
    maxPages: 5,
    filterKey: clientFilterKey,
    enabled: hasQuery && clientFilterActive,
  });

  const displayResults = SEARCH_SORT_ENABLED
    ? sortSearchResults(filteredResults, sort)
    : filteredResults;

  const clearClientFilters = () => {
    setFilters(
      {
        ...filters,
        genreFilter: emptyTriState(),
        languageFilter: emptyTriState(),
      },
      bounds,
    );
  };

  const clientFilterEmpty =
    !isLoading &&
    clientFilterActive &&
    displayResults.length === 0 &&
    results.length > 0 &&
    !hasNextPage;

  if (!hasQuery) {
    return (
      <>
        <div className="text-center mt-10">
          <h2>Searching..</h2>
        </div>
      </>
    );
  }

  return (
    <>
      <h2 className="page-container bg-secondary py-7 text-center text-4xl font-bold text-secondary-foreground">
        {getMediaTypeLabel(mediaType)} Search Results
      </h2>
      <hr className="opacity-20 text-muted-foreground horiz mb-11" />
      <div className="flex flex-col justify-center">
        <ListControls
          page="search"
          options={buildDiscoverSortOptions(mediaType)}
          disabled={!SEARCH_SORT_ENABLED}
        />
        {clientFilterActive ? (
          <p className="page-container text-sm text-muted-foreground">
            Filtering loaded results only
          </p>
        ) : null}
        <CardGrid className="page-container my-10 discoverPage">
          {isLoading ? (
            <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
          ) : clientFilterEmpty ? (
            <div className="col-span-full">
              <ClientFilterEmpty onClear={clearClientFilters} />
            </div>
          ) : (
            <CardPlate data={displayResults} mediaType={mediaType} />
          )}
          {isFetchingNextPage && (
            <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
          )}
        </CardGrid>
      </div>
    </>
  );
}
