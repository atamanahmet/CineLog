import { cn } from "@/lib/utils";

/** Auto-fill columns below sm (min 130px). Flex wrap from sm matches legacy catalog layout. */
export const CARD_GRID_BASE_CLASS =
  "grid w-full min-w-0 grid-cols-[repeat(auto-fill,minmax(130px,1fr))] items-stretch gap-3 sm:flex sm:flex-row sm:flex-wrap sm:items-stretch sm:justify-center sm:gap-5";

/**
 * Shared catalog card layout for list pages and skeletons.
 */
export default function CardGrid({ className, children, ...props }) {
  return (
    <div className={cn(CARD_GRID_BASE_CLASS, className)} {...props}>
      {children}
    </div>
  );
}
