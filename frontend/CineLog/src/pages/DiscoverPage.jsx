import CardGrid from "../components/CardGrid";
import CardPlate from "../components/CardPlate";
import { CardGridSkeletonItems } from "../components/CardGridSkeleton";
import ClientFilterEmpty from "../components/ClientFilterEmpty";
import ListControls from "../components/ListControls";
import useCatalogInfiniteQuery, {
  TMDB_PAGE_SIZE,
} from "../hooks/useCatalogInfiniteQuery";
import useEffectiveCatalogFilters from "../hooks/useEffectiveCatalogFilters";
import useInfiniteScrollTrigger from "../hooks/useInfiniteScrollTrigger";
import {
  emptyTriState,
  isTriStateActive,
} from "../lib/triStateFilter";
import { getMediaTypeLabel } from "../lib/mediaLabels";
import { buildDiscoverSortOptions } from "../lib/sortOptions";

/**
 * Discover catalog with infinite scroll.
 */
export default function DiscoverPage() {
  const {
    mediaType,
    queryFilters,
    filters,
    setFilters,
    bounds,
    sort,
  } = useEffectiveCatalogFilters("discover");
  const catalogFilters = { ...queryFilters, sort: sort || "" };
  const languageFilter = filters.languageFilter ?? emptyTriState();
  const { results, fetchNextPage, isFetching, isFetchingNextPage, isLoading } =
    useCatalogInfiniteQuery(
      ["discover", mediaType, catalogFilters],
      `/${mediaType}/discover`,
      {},
      {
        clientLanguageFilter: languageFilter,
        fillOnLanguageFilter: true,
      },
    );

  useInfiniteScrollTrigger(fetchNextPage, isFetching);

  const genreActive = isTriStateActive(queryFilters.genreFilter);
  const languageActive = isTriStateActive(languageFilter);
  const listDimmed =
    isFetching && !isLoading && !isFetchingNextPage && results.length > 0;
  const filterEmpty =
    !isLoading &&
    results.length === 0 &&
    (genreActive || languageActive) &&
    !isFetchingNextPage;

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

  return (
    <>
      <h2 className="page-container bg-secondary py-7 text-center text-4xl font-bold text-secondary-foreground">
        Discover {getMediaTypeLabel(mediaType)}
      </h2>
      <div className="flex flex-col justify-center">
        <ListControls
          page="discover"
          options={buildDiscoverSortOptions(mediaType)}
        />
        {languageActive ? (
          <p className="page-container text-sm text-muted-foreground">
            Filtering loaded results only
          </p>
        ) : null}
        <CardGrid
          className={`page-container my-10 discoverPage${listDimmed ? " opacity-60 transition-opacity" : ""}`}
        >
          {isLoading ? (
            <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
          ) : filterEmpty ? (
            <div className="col-span-full">
              <ClientFilterEmpty onClear={clearClientFilters} />
            </div>
          ) : (
            <CardPlate data={results} mediaType={mediaType} />
          )}
          {isFetchingNextPage && (
            <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
          )}
        </CardGrid>
      </div>
    </>
  );
}
