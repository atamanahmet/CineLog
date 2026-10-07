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

const NEW_RELEASES_EXTRAS = {
  releaseWindow: "2025-12-31",
};

/**
 * New releases catalog with infinite scroll.
 */
export default function NewReleasesPage() {
  const {
    mediaType,
    queryFilters,
    filters,
    setFilters,
    bounds,
    sort,
  } = useEffectiveCatalogFilters("newReleases");
  const extras = {
    ...NEW_RELEASES_EXTRAS,
    sort: sort || "release_date.desc",
  };
  const languageFilter = filters.languageFilter ?? emptyTriState();
  const { results, fetchNextPage, isFetching, isFetchingNextPage, isLoading } =
    useCatalogInfiniteQuery(
      ["new", mediaType, queryFilters, extras.sort],
      `/${mediaType}/discover`,
      extras,
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
        New Released {getMediaTypeLabel(mediaType)}
      </h2>
      <div className="flex flex-col justify-center">
        <ListControls
          page="newReleases"
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
