import { useEffect, useMemo } from "react";
import { useSearchParams } from "react-router";
import { RefreshCw } from "lucide-react";
import CardGrid from "../components/CardGrid";
import CardPlate from "../components/CardPlate";
import { CardGridSkeletonItems } from "../components/CardGridSkeleton";
import ProfileTabLabel from "../components/ProfileTabLabel";
import ProfileTabToolbar from "../components/ProfileTabToolbar";
import FilterSidebar from "../components/FilterSidebar";
import ClientFilterEmpty from "../components/ClientFilterEmpty";
import LoadingRegion from "../components/LoadingRegion";
import MediaTypeToggle from "../components/MediaTypeToggle";
import RejectedListBody from "../components/RejectedListBody";
import SortSelect from "../components/SortSelect";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import SimilarSection from "../features/settings/components/SimilarSection";
import useRecommendationSettings from "../features/settings/hooks/useRecommendationSettings";
import useDiscoverDefaults from "../hooks/useDiscoverDefaults";
import useDebouncedValue from "../hooks/useDebouncedValue";
import useFilteredRecommendations from "../hooks/useFilteredRecommendations";
import useFilteredSortedMedia from "../hooks/useFilteredSortedMedia";
import usePageFilters from "../hooks/usePageFilters";
import usePageMemory from "../hooks/usePageMemory";
import useRejectTitle from "../hooks/useRejectTitle";
import useRejectedList from "../hooks/useRejectedList";
import useRestoreRejected from "../hooks/useRestoreRejected";
import { TMDB_PAGE_SIZE } from "../hooks/useCatalogInfiniteQuery";
import { filterScopeKey } from "../lib/filterScope";
import {
  emptyTriState,
  isTriStateActive,
} from "../lib/triStateFilter";
import { genreChipsForView } from "../lib/genreLabels";
import {
  LIST_FILTER_ACTIVE_KEYS,
  filterListItems,
  isListFilterNarrowed,
} from "../lib/listFilterKeys";
import { recFilterActiveKeys } from "../lib/recFilterKeys";
import { RECOMMENDATION_SORT_OPTIONS } from "../lib/sortOptions";
import { recommendationSkeletonCount } from "../lib/recommendationSkeleton";
import { useAuthStore } from "../stores/authStore";
import { useAuthModalStore } from "../stores/authModalStore";
import { useListsStore } from "../stores/listsStore";
import { useSimilarStore } from "../stores/similarStore";
import { useUserPrefsStore } from "../stores/userPrefsStore";
import { parseProfileTab } from "../lib/parseProfileTab";
import { TABS } from "../lib/profileTabs";

const PROFILE_TAB_KEYS = TABS.map((tab) => tab.value);
const DEFAULT_PROFILE_TAB = "Watchlist";

const SIDEBAR_TABS = new Set([
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
  "Recommendation",
  "Find similar",
]);

const LIST_TABS = new Set([
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
]);

const EMPTY_TEXT = {
  movie: "No movie recommendations yet.",
  tv: "No TV recommendations yet.",
  all: "No recommendations yet.",
};

const FILTER_EMPTY_TEXT =
  "No titles match these filters. Try loosening them.";

const GENRE_DEBOUNCE_MS = 600;

/**
 * Recommendation count for the profile tab badge.
 */
function recommendationCount(view, recommendations) {
  if (view === "all") {
    const movies = recommendations.movie;
    const tv = recommendations.tv;
    if (movies == null && tv == null) {
      return null;
    }
    return (movies?.length ?? 0) + (tv?.length ?? 0);
  }
  const list = recommendations[view];
  return Array.isArray(list) ? list.length : null;
}

/**
 * Profile lists, recommendations, and find similar.
 */
export default function ProfilePage() {
  const user = useAuthStore((s) => s.user);
  const profilePhotoUrl = useAuthStore((s) => s.profilePhotoUrl);
  const openModal = useAuthModalStore((s) => s.openModal);

  const watchlist = useListsStore((s) => s.watchlist);
  const lovedlist = useListsStore((s) => s.lovedlist);
  const watchedlist = useListsStore((s) => s.watchedlist);
  const listsReady = useListsStore((s) => s.listsReady);
  const recommendations = useListsStore((s) => s.recommendations);
  const recommendationErrors = useListsStore((s) => s.recommendationErrors);
  const recommendationLoading = useListsStore((s) => s.recommendationLoading);
  const getAllRecommendations = useListsStore((s) => s.getAllRecommendations);
  const getRecommendation = useListsStore((s) => s.getRecommendation);
  const getWatchList = useListsStore((s) => s.getWatchList);

  const { settingsQuery } = useRecommendationSettings();
  const maxResults = settingsQuery.data?.maxResults ?? null;

  const [searchParams, setSearchParams] = useSearchParams();
  const selection = parseProfileTab(
    searchParams.get("tab"),
    PROFILE_TAB_KEYS,
    DEFAULT_PROFILE_TAB,
  );
  const scopeKey = filterScopeKey("/profile", selection);
  usePageMemory(scopeKey);

  const {
    data: bounds,
    isLoading: defaultsLoading,
    isError: defaultsError,
    refetch: refetchDefaults,
    isFetching: defaultsFetching,
  } = useDiscoverDefaults();

  const setProfileTab = useUserPrefsStore((s) => s.setProfileTab);
  const recommendationView = useUserPrefsStore((s) => s.recommendationView);
  const setRecommendationView = useUserPrefsStore(
    (s) => s.setRecommendationView,
  );
  const similarView = useSimilarStore((s) => s.view);
  const sortKey =
    useUserPrefsStore((s) => s.sortByPage?.recommendation ?? "match");
  const setSortForPage = useUserPrefsStore((s) => s.setSortForPage);

  const { filters: recFilters, setFilters: setRecFilters } = usePageFilters(
    "Recommendation",
    recommendationView,
    "Recommendation",
  );
  const genreFilter = recFilters.genreFilter ?? emptyTriState();
  const debouncedGenreFilter = useDebouncedValue(genreFilter, GENRE_DEBOUNCE_MS);
  const immediateGenreActive = isTriStateActive(genreFilter);
  const debouncedGenreActive = isTriStateActive(debouncedGenreFilter);
  const useFilteredPath =
    selection === "Recommendation" &&
    immediateGenreActive &&
    debouncedGenreActive;

  const filteredRecQuery = useFilteredRecommendations(
    recommendationView,
    debouncedGenreFilter,
    selection === "Recommendation",
  );

  const movieSource = useFilteredPath
    ? filteredRecQuery.movies
    : recommendations.movie;
  const tvSource = useFilteredPath ? filteredRecQuery.tv : recommendations.tv;

  const recActiveKeys = recFilterActiveKeys(recommendationView);
  const filteredRecommendations = useFilteredSortedMedia({
    movies: movieSource,
    tv: tvSource,
    view: recommendationView,
    sortKey,
    activeKeys: recActiveKeys,
    bounds,
    scopeKey: "Recommendation",
    mediaType: recommendationView,
  });
  const displayedRecommendation = filteredRecommendations.items;
  const listDimmed =
    useFilteredPath &&
    filteredRecQuery.isFetching &&
    !filteredRecQuery.isLoading;
  const rejectTitle = useRejectTitle();
  const restoreTitle = useRestoreRejected();
  const rejectedList = useRejectedList();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const isListTab = LIST_TABS.has(selection);
  const { filters: listFilters, setFilters: setListFilters } = usePageFilters(
    isListTab ? selection : "Watchlist",
    storeMediaType,
    isListTab ? selection : "Watchlist",
  );
  const anyLoading = useFilteredPath
    ? filteredRecQuery.isFetching
    : recommendationLoading.movie || recommendationLoading.tv;

  const clearClientFilters = () => {
    if (isListTab) {
      setListFilters(
        {
          ...listFilters,
          genreFilter: emptyTriState(),
          languageFilter: emptyTriState(),
        },
        bounds,
      );
      return;
    }
    setRecFilters(
      {
        ...recFilters,
        genreFilter: emptyTriState(),
        languageFilter: emptyTriState(),
      },
      bounds,
    );
  };

  const dataMap = {
    Watchlist: watchlist,
    Loved: lovedlist,
    Watchedlist: watchedlist,
    Recommendation: displayedRecommendation,
  };

  const rawListItems = useMemo(() => {
    if (!isListTab) {
      return [];
    }
    if (selection === "Not interested") {
      return rejectedList.items ?? [];
    }
    if (selection === "Watchlist") {
      return watchlist;
    }
    if (selection === "Loved") {
      return lovedlist;
    }
    if (selection === "Watchedlist") {
      return watchedlist;
    }
    return [];
  }, [
    isListTab,
    selection,
    rejectedList.items,
    watchlist,
    lovedlist,
    watchedlist,
  ]);

  const filteredListItems = useMemo(() => {
    if (!isListTab || !bounds) {
      return rawListItems;
    }
    return filterListItems(rawListItems, listFilters, bounds);
  }, [isListTab, bounds, rawListItems, listFilters]);

  const listGenreActive = isTriStateActive(
    listFilters.genreFilter ?? emptyTriState(),
  );
  const listLanguageActive = isTriStateActive(
    listFilters.languageFilter ?? emptyTriState(),
  );
  const listClientFilterActive = listGenreActive || listLanguageActive;
  const listClientFilterEmpty =
    isListTab &&
    listsReady &&
    listClientFilterActive &&
    filteredListItems.length === 0 &&
    rawListItems.length > 0;
  const listNarrowed =
    isListTab && bounds != null && isListFilterNarrowed(listFilters, bounds);

  const listHasTv = rawListItems.some(
    (item) => String(item?.mediaType ?? "").toUpperCase() === "TV",
  );
  const listGenreChips = genreChipsForView(listHasTv ? "all" : "movie");

  const countMap = {
    Watchlist: watchlist.length,
    Watchedlist: watchedlist.length,
    Loved: lovedlist.length,
    "Not interested": rejectedList.hasData ? rejectedList.items.length : null,
    Recommendation: recommendationCount(recommendationView, recommendations),
  };

  const dataSet = isListTab
    ? filteredListItems
    : dataMap[selection] || null;

  useEffect(() => {
    if (user && !listsReady) {
      getWatchList();
    }
  }, [user, listsReady, getWatchList]);

  useEffect(() => {
    if (!user || selection !== "Recommendation") {
      return;
    }
    getAllRecommendations();
  }, [user, selection, getAllRecommendations]);

  useEffect(() => {
    setProfileTab(selection);
  }, [selection, setProfileTab]);

  function handleTabChange(value) {
    setProfileTab(value);
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set("tab", value);
        return next;
      },
      { replace: true },
    );
  }

  function handleRefresh() {
    if (useFilteredPath) {
      filteredRecQuery.movieQuery?.refetch();
      filteredRecQuery.tvQuery?.refetch();
      return;
    }
    getAllRecommendations();
  }

  function renderInlineTypeError(type) {
    if (!recommendationErrors[type]) {
      return null;
    }
    const label = type === "tv" ? "TV" : "movie";
    return (
      <div className="mb-4 w-full text-center">
        <p className="text-foreground">
          Could not load {label} recommendations
        </p>
        <Button
          variant="outline"
          onClick={() => getRecommendation(type)}
          className="mt-2"
        >
          Retry {label}
        </Button>
      </div>
    );
  }

  function renderRecommendationSkeleton() {
    const count = recommendationSkeletonCount(maxResults);
    if (count == null) {
      return (
        <LoadingRegion label="Loading recommendations" className="w-full py-8">
          <Skeleton className="mx-auto h-4 w-40" />
        </LoadingRegion>
      );
    }
    return <CardGridSkeletonItems count={count} />;
  }

  function renderFilterEmpty() {
    return (
      <p className="w-full text-center text-foreground">{FILTER_EMPTY_TEXT}</p>
    );
  }

  function renderGenreEmpty() {
    return <ClientFilterEmpty onClear={clearClientFilters} className="flex w-full flex-col items-center gap-3 text-center" />;
  }

  function wrapDimmed(node) {
    if (!listDimmed) {
      return node;
    }
    return <div className="opacity-60 transition-opacity">{node}</div>;
  }

  function renderRecommendationBody() {
    if (useFilteredPath) {
      return renderFilteredRecommendationBody();
    }

    if (recommendationView === "all") {
      const movies = recommendations.movie;
      const tv = recommendations.tv;
      const bothMissing = movies == null && tv == null;
      const movieFailed = recommendationErrors.movie && movies == null;
      const tvFailed = recommendationErrors.tv && tv == null;

      if (bothMissing && (movieFailed || tvFailed)) {
        return (
          <div className="w-full text-center">
            {movieFailed && renderInlineTypeError("movie")}
            {tvFailed && renderInlineTypeError("tv")}
          </div>
        );
      }

      if (bothMissing) {
        return renderRecommendationSkeleton();
      }

      const total = filteredRecommendations.total;
      const shown = filteredRecommendations.shown;
      const empty =
        Array.isArray(movies) &&
        Array.isArray(tv) &&
        total === 0 &&
        !recommendationErrors.movie &&
        !recommendationErrors.tv;

      return (
        <>
          {recommendationErrors.movie && renderInlineTypeError("movie")}
          {recommendationErrors.tv && renderInlineTypeError("tv")}
          {empty ? (
            <p className="w-full text-center text-foreground">
              {EMPTY_TEXT.all}
            </p>
          ) : total > 0 && shown === 0 ? (
            renderFilterEmpty()
          ) : (
            <CardPlate
              data={displayedRecommendation}
              onReject={rejectTitle}
            />
          )}
        </>
      );
    }

    const type = recommendationView;
    const list = recommendations[type];
    const failed = recommendationErrors[type];

    if (list == null && failed) {
      return (
        <div className="text-center">
          <h2 className="text-foreground">Could not load recommendations</h2>
          <Button
            variant="outline"
            onClick={() => getRecommendation(type)}
            className="mt-3"
          >
            Retry
          </Button>
        </div>
      );
    }

    if (list == null) {
      return renderRecommendationSkeleton();
    }

    if (list.length === 0) {
      return (
        <p className="w-full text-center text-foreground">
          {EMPTY_TEXT[type]}
        </p>
      );
    }

    if (filteredRecommendations.shown === 0) {
      return renderFilterEmpty();
    }

    return (
      <>
        {failed && renderInlineTypeError(type)}
        <CardPlate
          data={displayedRecommendation}
          mediaType={type}
          onReject={rejectTitle}
        />
      </>
    );
  }

  function renderFilteredTypeError(type, query) {
    if (!query?.isError) {
      return null;
    }
    const label = type === "tv" ? "TV" : "movie";
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

  function renderFilteredRecommendationBody() {
    if (recommendationView === "all") {
      const movies = filteredRecQuery.movies;
      const tv = filteredRecQuery.tv;
      const movieQ = filteredRecQuery.movieQuery;
      const tvQ = filteredRecQuery.tvQuery;
      const bothMissing = movies == null && tv == null;
      const movieFailed = movieQ?.isError && movies == null;
      const tvFailed = tvQ?.isError && tv == null;

      if (bothMissing && (movieFailed || tvFailed)) {
        return (
          <div className="w-full text-center">
            {movieFailed && renderFilteredTypeError("movie", movieQ)}
            {tvFailed && renderFilteredTypeError("tv", tvQ)}
          </div>
        );
      }

      if (bothMissing || filteredRecQuery.isLoading) {
        return renderRecommendationSkeleton();
      }

      const total = filteredRecommendations.total;
      const shown = filteredRecommendations.shown;
      const empty =
        Array.isArray(movies) &&
        Array.isArray(tv) &&
        total === 0 &&
        !movieQ?.isError &&
        !tvQ?.isError;

      return wrapDimmed(
        <>
          {movieQ?.isError && renderFilteredTypeError("movie", movieQ)}
          {tvQ?.isError && renderFilteredTypeError("tv", tvQ)}
          {empty ? (
            renderGenreEmpty()
          ) : total > 0 && shown === 0 ? (
            renderFilterEmpty()
          ) : (
            <CardPlate
              data={displayedRecommendation}
              onReject={rejectTitle}
            />
          )}
        </>,
      );
    }

    const type = recommendationView;
    const query =
      type === "tv" ? filteredRecQuery.tvQuery : filteredRecQuery.movieQuery;
    const list = type === "tv" ? filteredRecQuery.tv : filteredRecQuery.movies;
    const failed = query?.isError;

    if (list == null && failed) {
      return (
        <div className="text-center">
          <h2 className="text-foreground">Could not load recommendations</h2>
          <Button
            variant="outline"
            onClick={() => query.refetch()}
            className="mt-3"
          >
            Retry
          </Button>
        </div>
      );
    }

    if (list == null || filteredRecQuery.isLoading) {
      return renderRecommendationSkeleton();
    }

    if (list.length === 0) {
      return renderGenreEmpty();
    }

    if (filteredRecommendations.shown === 0) {
      return wrapDimmed(renderFilterEmpty());
    }

    return wrapDimmed(
      <>
        {failed && renderFilteredTypeError(type, query)}
        <CardPlate
          data={displayedRecommendation}
          mediaType={type}
          onReject={rejectTitle}
        />
      </>,
    );
  }

  function renderListBody() {
    if (!listsReady) {
      return <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />;
    }
    if (listClientFilterEmpty) {
      return renderGenreEmpty();
    }
    if (
      listNarrowed &&
      filteredListItems.length === 0 &&
      rawListItems.length > 0 &&
      !listGenreActive
    ) {
      return renderFilterEmpty();
    }
    return <CardPlate data={dataSet} />;
  }

  function renderRejectedBody() {
    if (listClientFilterEmpty) {
      return renderGenreEmpty();
    }
    if (
      listNarrowed &&
      filteredListItems.length === 0 &&
      rawListItems.length > 0 &&
      !listGenreActive
    ) {
      return renderFilterEmpty();
    }
    return (
      <RejectedListBody
        items={filteredListItems}
        hasData={rejectedList.hasData}
        isLoading={rejectedList.isLoading}
        isError={rejectedList.isError}
        refetch={rejectedList.refetch}
        onRestore={restoreTitle}
      />
    );
  }

  function renderShowingLine() {
    if (selection === "Recommendation") {
      const { narrowed, shown, total } = filteredRecommendations;
      if (!narrowed || shown >= total || total === 0) {
        return null;
      }
      return (
        <p className="text-sm text-muted-foreground">
          Showing {shown} of {total}
        </p>
      );
    }
    if (isListTab && listNarrowed && rawListItems.length > 0) {
      if (filteredListItems.length >= rawListItems.length) {
        return null;
      }
      return (
        <p className="text-sm text-muted-foreground">
          Showing {filteredListItems.length} of {rawListItems.length}
        </p>
      );
    }
    return null;
  }

  function renderContent() {
    if (selection === "Find similar") {
      return <SimilarSection bounds={bounds} />;
    }

    return (
      <div className="page-container flex min-w-0 flex-col">
        <div
          className={
            selection === "Recommendation"
              ? "flex w-full min-w-0 flex-col gap-3 border-b border-border py-4 md:flex-row md:items-center md:justify-between md:gap-2"
              : "w-full min-w-0 border-b border-border py-4"
          }
        >
          <h2 className="w-full min-w-0 text-2xl font-bold text-foreground">
            {selection}
          </h2>
          {selection === "Recommendation" && (
            <ProfileTabToolbar
              mediaToggle={
                <MediaTypeToggle
                  allowAll
                  mediaType={recommendationView}
                  onChange={setRecommendationView}
                />
              }
              sortSelect={
                <SortSelect
                  ariaLabel="Sort by"
                  className="w-full md:w-48"
                  value={sortKey}
                  options={RECOMMENDATION_SORT_OPTIONS}
                  onValueChange={(next) =>
                    setSortForPage("recommendation", next)
                  }
                />
              }
              trailingAction={
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="shrink-0"
                  aria-label="Refresh recommendations"
                  disabled={anyLoading}
                  onClick={handleRefresh}
                >
                  <RefreshCw
                    className={anyLoading ? "animate-spin" : undefined}
                  />
                </Button>
              }
            />
          )}
        </div>
        {renderShowingLine()}
        <CardGrid className="my-8 w-full min-w-0">
          {selection === "Recommendation"
            ? renderRecommendationBody()
            : selection === "Not interested"
              ? renderRejectedBody()
              : renderListBody()}
        </CardGrid>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="page-container mt-10 text-center">
        <h2 className="text-foreground">
          Please{" "}
          <button
            type="button"
            className="font-bold italic text-primary"
            onClick={() => openModal("login")}
          >
            login...
          </button>
        </h2>
      </div>
    );
  }

  const headerLoading = !listsReady;
  const showSidebar = SIDEBAR_TABS.has(selection);
  const filterView =
    selection === "Find similar" ? similarView : recommendationView;
  const sidebarActiveKeys =
    selection === "Recommendation" || selection === "Find similar"
      ? recFilterActiveKeys(filterView)
      : isListTab
        ? LIST_FILTER_ACTIVE_KEYS
        : undefined;
  const sidebarGenreMediaType =
    selection === "Recommendation" || selection === "Find similar"
      ? filterView
      : undefined;
  const sidebarGenreChips = isListTab ? listGenreChips : undefined;

  return (
    <div className="flex flex-col">
      <div className="page-container flex flex-col gap-6 py-8 sm:flex-row sm:items-center sm:justify-between">
        {headerLoading ? (
          <LoadingRegion
            label="Loading profile"
            className="flex items-center gap-4"
          >
            <Skeleton shell className="size-20 rounded-full" />
            <Skeleton className="h-8 w-40" />
          </LoadingRegion>
        ) : (
          <div className="flex items-center gap-4">
            <Avatar className="size-20 border-2 border-primary">
              <AvatarImage src={profilePhotoUrl || undefined} alt={user} />
              <AvatarFallback className="bg-card text-2xl text-foreground">
                {user?.charAt(0)?.toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <h3 className="text-2xl font-semibold text-foreground">{user}</h3>
          </div>
        )}

        <div className="min-w-0 w-full md:w-auto">
          <div className="w-full max-w-full sm:w-72 md:hidden">
            <Select value={selection} onValueChange={handleTabChange}>
              <SelectTrigger
                aria-label="Profile section"
                className="h-11 w-full rounded-md border border-border bg-card text-sm font-medium text-foreground shadow-none"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent
                position="popper"
                sideOffset={4}
                className="w-[var(--radix-select-trigger-width)] rounded-md border border-border bg-popover text-popover-foreground shadow-md"
              >
                {TABS.map((tab) => (
                  <SelectItem
                    key={tab.value}
                    value={tab.value}
                    className="group min-h-11 cursor-pointer py-2.5"
                  >
                    <span className="flex w-full items-center justify-between gap-2 pr-2">
                      <ProfileTabLabel
                        label={tab.label}
                        count={listsReady ? countMap[tab.value] : null}
                      />
                    </span>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Tabs
            value={selection}
            onValueChange={handleTabChange}
            className="hidden min-w-0 w-full md:block md:w-auto"
          >
            <TabsList className="flex h-auto w-full max-w-full flex-wrap gap-2 overflow-visible bg-transparent p-0">
              {TABS.map((tab) => (
                <TabsTrigger
                  key={tab.value}
                  value={tab.value}
                  className="group shrink-0 gap-2 rounded-2xl border border-border bg-secondary/60 px-4 py-2 text-foreground data-[state=active]:border-primary data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
                >
                  <ProfileTabLabel
                    label={tab.label}
                    count={listsReady ? countMap[tab.value] : null}
                  />
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        </div>
      </div>

      {showSidebar ? (
        defaultsLoading ? (
          <CardGrid className="page-container my-10 w-full min-w-0">
            <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
          </CardGrid>
        ) : defaultsError || !bounds ? (
          <main className="page-container my-10 flex flex-col items-center gap-3 text-center">
            <p className="text-sm text-muted-foreground">
              Could not load filter defaults.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => refetchDefaults()}
              disabled={defaultsFetching}
            >
              Retry
            </Button>
          </main>
        ) : (
          <FilterSidebar
            bounds={bounds}
            activeKeys={sidebarActiveKeys}
            genreMediaType={sidebarGenreMediaType}
            genreChips={sidebarGenreChips}
          >
            {renderContent()}
          </FilterSidebar>
        )
      ) : (
        renderContent()
      )}
    </div>
  );
}
