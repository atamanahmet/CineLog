import { CardSkeleton } from "./Card";
import CardGrid from "./CardGrid";
import { cn } from "@/lib/utils";

/**
 * Skeleton cards only, for use inside an existing CardGrid.
 */
export function CardGridSkeletonItems({ count }) {
  return (
    <>
      <span className="sr-only">Loading</span>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="relative h-full w-full min-w-0 sm:mb-8 sm:w-auto">
          <CardSkeleton />
        </div>
      ))}
    </>
  );
}

/**
 * Catalog card grid placeholder. Count must match page size or max results.
 */
export default function CardGridSkeleton({ count, className, itemsOnly = false }) {
  const items = <CardGridSkeletonItems count={count} />;

  if (itemsOnly) {
    return <div aria-busy="true">{items}</div>;
  }

  return (
    <CardGrid aria-busy="true" className={cn(className)}>
      {items}
    </CardGrid>
  );
}
