import { useState } from "react";
import { X } from "lucide-react";
import CardPlate from "../../../components/CardPlate";
import CardGrid from "../../../components/CardGrid";
import { CardGridSkeletonItems } from "../../../components/CardGridSkeleton";
import ClientFilterEmpty from "../../../components/ClientFilterEmpty";
import ProfileTabToolbar from "../../../components/ProfileTabToolbar";
import LoadingRegion from "../../../components/LoadingRegion";
import MediaTypeToggle from "../../../components/MediaTypeToggle";
import SimilarSeedDialog from "../../../components/SimilarSeedDialog";
import useRecommendationSettings from "../hooks/useRecommendationSettings";
import useDebouncedValue from "../../../hooks/useDebouncedValue";
import useFilteredSortedMedia from "../../../hooks/useFilteredSortedMedia";
import usePageFilters from "../../../hooks/usePageFilters";
import useRejectTitle from "../../../hooks/useRejectTitle";
import { useSimilarRecommendations } from "../../../hooks/useSimilarRecommendations";
import {
  emptyTriState,
  isTriStateActive,
} from "../../../lib/triStateFilter";
import { recFilterActiveKeys } from "../../../lib/recFilterKeys";
import { RECOMMENDATION_SORT_OPTIONS } from "../../../lib/sortOptions";
import { recommendationSkeletonCount } from "../../../lib/recommendationSkeleton";
import SortSelect from "../../../components/SortSelect";
import { useSimilarStore } from "../../../stores/similarStore";
import { similarEmptyMessage } from "../../../utils/similarMessages";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

const IDLE_TEXT =
  "Pick a few movies or shows you like and we will find similar ones.";

const FILTER_EMPTY_TEXT =
  "No titles match these filters. Try loosening them.";

const GENRE_DEBOUNCE_MS = 600;

/**
 * Profile tab: pick seeds, then show similar titles.
 */
export default function SimilarSection({ bounds }) {
  const seeds = useSimilarStore((s) => s.seeds);
  const view = useSimilarStore((s) => s.view);
  const setView = useSimilarStore((s) => s.setView);
  const removeSeed = useSimilarStore((s) => s.removeSeed);
  const clearSeeds = useSimilarStore((s) => s.clearSeeds);
  const [modalOpen, setModalOpen] = useState(false);
  const [sortKey, setSortKey] = useState("match");
  const { settingsQuery } = useRecommendationSettings();
  const maxResults = settingsQuery.data?.maxResults ?? null;

  const { filters, setFilters } = usePageFilters(
    "Find similar",
    view,
    "Find similar",
  );
  const genreFilter = filters.genreFilter ?? emptyTriState();
  const debouncedGenreFilter = useDebouncedValue(genreFilter, GENRE_DEBOUNCE_MS);
  const immediateGenreActive = isTriStateActive(genreFilter);
  const debouncedGenreActive = isTriStateActive(debouncedGenreFilter);
  const genrePathActive = immediateGenreActive && debouncedGenreActive;

  const similar = useSimilarRecommendations(
    seeds,
    view,
    modalOpen,
    debouncedGenreFilter,
  );
  const rejectTitle = useRejectTitle();

  const movieItems =
    view === "all"
      ? (similar.movieQuery?.data ?? null)
      : view === "movie"
        ? similar.items
        : null;
  const tvItems =
    view === "all"
      ? (similar.tvQuery?.data ?? null)
      : view === "tv"
        ? similar.items
        : null;

  const filtered = useFilteredSortedMedia({
    movies: movieItems,
    tv: tvItems,
    view,
    sortKey,
    activeKeys: recFilterActiveKeys(view),
    bounds,
    scopeKey: "Find similar",
    mediaType: view,
  });
  const displayed = filtered.items;
  const listDimmed =
    genrePathActive && similar.isFetching && !similar.isLoading;

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

  function renderInlineTypeError(type, query) {
    if (!query?.isError) {
      return null;
    }
    const label = type === "TV" ? "TV" : "movie";
    return (
      <div className="mb-4 w-full text-center">
        <p className="text-foreground">
          Could not load {label} recommendations
        </p>
        <Button
          variant="outline"
          onClick={() => query.refetch()}
          className="mt-2"
        >
          Retry {label}
        </Button>
      </div>
    );
  }

  function renderSkeleton() {
    if (maxResults == null) {
      return (
        <LoadingRegion label="Loading similar titles" className="w-full py-8">
          <Skeleton className="mx-auto h-4 w-40" />
        </LoadingRegion>
      );
    }
    return (
      <CardGridSkeletonItems count={recommendationSkeletonCount(maxResults)} />
    );
  }

  function renderFilterEmpty() {
    return (
      <p className="w-full text-center text-foreground">{FILTER_EMPTY_TEXT}</p>
    );
  }

  function renderGenreEmpty() {
    return (
      <ClientFilterEmpty
        onClear={clearClientFilters}
        className="flex w-full flex-col items-center gap-3 text-center"
      />
    );
  }

  function wrapDimmed(node) {
    if (!listDimmed) {
      return node;
    }
    return <div className="opacity-60 transition-opacity">{node}</div>;
  }

  function renderResultsBody() {
    if (seeds.length === 0) {
      return null;
    }

    if (view === "all") {
      const movieQ = similar.movieQuery;
      const tvQ = similar.tvQuery;
      const movies = movieQ?.data;
      const tv = tvQ?.data;
      const bothMissing = movies == null && tv == null;
      const movieFailed = movieQ?.isError && movies == null;
      const tvFailed = tvQ?.isError && tv == null;

      if (bothMissing && (movieFailed || tvFailed)) {
        return (
          <div className="w-full text-center">
            {movieFailed && renderInlineTypeError("MOVIE", movieQ)}
            {tvFailed && renderInlineTypeError("TV", tvQ)}
          </div>
        );
      }

      if (bothMissing) {
        return renderSkeleton();
      }

      const empty =
        Array.isArray(movies) &&
        Array.isArray(tv) &&
        filtered.total === 0 &&
        !movieQ?.isError &&
        !tvQ?.isError &&
        similar.bothSettled;

      return wrapDimmed(
        <>
          {movieQ?.isError && renderInlineTypeError("MOVIE", movieQ)}
          {tvQ?.isError && renderInlineTypeError("TV", tvQ)}
          {empty ? (
            genrePathActive ? (
              renderGenreEmpty()
            ) : (
              <p className="w-full text-center text-foreground">
                {similarEmptyMessage(seeds.length)}
              </p>
            )
          ) : filtered.total > 0 && filtered.shown === 0 ? (
            renderFilterEmpty()
          ) : (
            <CardPlate data={displayed} onReject={rejectTitle} />
          )}
        </>,
      );
    }

    const type = view === "tv" ? "TV" : "MOVIE";
    const query = view === "tv" ? similar.tvQuery : similar.movieQuery;
    const list = similar.items;
    const failed = similar.isError;

    if (list == null && failed) {
      return (
        <div className="text-center">
          <h2 className="text-foreground">Could not load recommendations</h2>
          <Button
            variant="outline"
            onClick={() => similar.refetch()}
            className="mt-3"
          >
            Retry
          </Button>
        </div>
      );
    }

    if (list == null) {
      return renderSkeleton();
    }

    if (list.length === 0) {
      return genrePathActive ? (
        renderGenreEmpty()
      ) : (
        <p className="w-full text-center text-foreground">
          {similarEmptyMessage(seeds.length)}
        </p>
      );
    }

    if (filtered.shown === 0) {
      return wrapDimmed(renderFilterEmpty());
    }

    return wrapDimmed(
      <>
        {query?.isError && renderInlineTypeError(type, query)}
        <CardPlate
          data={displayed}
          mediaType={view}
          onReject={rejectTitle}
        />
      </>,
    );
  }

  function renderShowingLine() {
    if (seeds.length === 0) {
      return null;
    }
    const { narrowed, shown, total } = filtered;
    if (!narrowed || shown >= total || total === 0) {
      return null;
    }
    return (
      <p className="text-sm text-muted-foreground">
        Showing {shown} of {total}
      </p>
    );
  }

  return (
    <div className="page-container flex min-w-0 flex-col">
      <div className="flex w-full min-w-0 flex-col gap-3 border-b border-border py-4 md:flex-row md:items-center md:justify-between md:gap-2">
        <h2 className="w-full min-w-0 text-2xl font-bold text-foreground">
          Find similar
        </h2>
        <ProfileTabToolbar
          mediaToggle={
            <MediaTypeToggle
              allowAll
              mediaType={view}
              onChange={setView}
            />
          }
          sortSelect={
            <SortSelect
              ariaLabel="Sort by"
              className="w-full md:w-48"
              value={sortKey}
              options={RECOMMENDATION_SORT_OPTIONS}
              onValueChange={setSortKey}
            />
          }
          trailingAction={
            <Button
              type="button"
              variant="outline"
              className="shrink-0"
              onClick={() => setModalOpen(true)}
            >
              Add titles
            </Button>
          }
        />
      </div>
      {renderShowingLine()}

      <div className="mt-6">
        {seeds.length === 0 ? (
          <p className="w-full text-center text-foreground">{IDLE_TEXT}</p>
        ) : (
          <div className="flex flex-wrap items-center gap-2">
            {seeds.map((seed) => (
              <span
                key={`${seed.mediaType}:${seed.tmdbId}`}
                className="inline-flex max-w-full items-center gap-1 rounded-full border border-border bg-secondary/60 px-3 py-1 text-sm text-foreground"
              >
                <span className="truncate">{seed.title}</span>
                <button
                  type="button"
                  aria-label={`Remove ${seed.title}`}
                  className="rounded-full p-0.5 text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                  onClick={() => removeSeed(seed.tmdbId, seed.mediaType)}
                >
                  <X className="size-3.5" />
                </button>
              </span>
            ))}
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={clearSeeds}
            >
              Clear all
            </Button>
          </div>
        )}
      </div>

      <CardGrid className="my-8 w-full min-w-0">
        {renderResultsBody()}
      </CardGrid>

      <SimilarSeedDialog open={modalOpen} onOpenChange={setModalOpen} />
    </div>
  );
}
