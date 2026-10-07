import { Outlet, useLocation } from "react-router";
import CardGridSkeleton from "./CardGridSkeleton";
import FilterSidebar from "./FilterSidebar";
import useDiscoverDefaults from "../hooks/useDiscoverDefaults";
import usePageMemory from "../hooks/usePageMemory";
import { TMDB_PAGE_SIZE } from "../hooks/useCatalogInfiniteQuery";
import { filterScopeKey } from "../lib/filterScope";
import { Button } from "@/components/ui/button";

/**
 * Wait for discover defaults, then render filter chrome and child routes.
 */
export default function FilterLayout() {
  const location = useLocation();
  const scopeKey = filterScopeKey(location.pathname);
  usePageMemory(scopeKey);

  const { data: bounds, isLoading, isError, refetch, isFetching } =
    useDiscoverDefaults();

  if (isLoading) {
    return (
      <CardGridSkeleton
        count={TMDB_PAGE_SIZE}
        className="page-container my-10"
      />
    );
  }

  if (isError || !bounds) {
    return (
      <main className="page-container my-10 flex flex-col items-center gap-3 text-center">
        <p className="text-sm text-muted-foreground">
          Could not load filter defaults.
        </p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          disabled={isFetching}
        >
          Retry
        </Button>
      </main>
    );
  }

  return (
    <FilterSidebar bounds={bounds}>
      <Outlet />
    </FilterSidebar>
  );
}
