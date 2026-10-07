import CardPlate from "./CardPlate";
import { CardGridSkeletonItems } from "./CardGridSkeleton";
import LoadingRegion from "./LoadingRegion";
import { Button } from "@/components/ui/button";
import { TMDB_PAGE_SIZE } from "../hooks/useCatalogInfiniteQuery";

const EMPTY_TEXT =
  "Titles you mark as not interested will show here.";

/**
 * Body for the Not interested profile tab.
 */
export default function RejectedListBody({
  items,
  hasData,
  isLoading,
  isError,
  refetch,
  onRestore,
}) {
  if (isLoading && !hasData) {
    return (
      <LoadingRegion label="Loading not interested" className="w-full py-8">
        <CardGridSkeletonItems count={TMDB_PAGE_SIZE} />
      </LoadingRegion>
    );
  }

  if (isError && !hasData) {
    return (
      <div className="w-full text-center">
        <p className="text-foreground">Could not load this list.</p>
        <Button variant="outline" onClick={() => refetch()} className="mt-3">
          Try again
        </Button>
      </div>
    );
  }

  if (!items?.length) {
    return (
      <p className="w-full text-center text-foreground">{EMPTY_TEXT}</p>
    );
  }

  return (
    <CardPlate
      data={items}
      onReject={onRestore}
      rejectActive
    />
  );
}
