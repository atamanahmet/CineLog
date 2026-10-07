import { useEffect } from "react";

/**
 * True when the sentinel should trigger another page fetch.
 */
export function shouldFetchNextPage({
  isIntersecting,
  hasNextPage,
  isFetchingNextPage,
  pagesLoaded,
  maxPages,
}) {
  return (
    Boolean(isIntersecting) &&
    Boolean(hasNextPage) &&
    !isFetchingNextPage &&
    pagesLoaded < maxPages
  );
}

/**
 * Observe a sentinel inside a non-window scroll root.
 */
export default function useInfiniteScrollTrigger(
  sentinelRef,
  rootRef,
  onIntersect,
  enabled = true,
) {
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    const sentinel = sentinelRef?.current;
    const root = rootRef?.current;
    if (!sentinel || !root) {
      return undefined;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          onIntersect();
        }
      },
      { root, rootMargin: "0px 0px 200px 0px" },
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [sentinelRef, rootRef, onIntersect, enabled]);
}
