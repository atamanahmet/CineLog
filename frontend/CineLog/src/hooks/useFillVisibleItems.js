import { useEffect, useRef } from "react";
import { shouldFetchMore } from "./shouldFetchMore";

/**
 * Fetch more pages while a client filter leaves fewer than minVisible items.
 * Hard cap of maxPages extra fetches; counter resets when filterKey changes.
 */
export default function useFillVisibleItems({
  visibleCount,
  hasNextPage,
  isFetching,
  fetchNextPage,
  minVisible = 20,
  maxPages = 5,
  filterKey,
  enabled = true,
}) {
  const extraPagesRef = useRef(0);
  const prevKeyRef = useRef(filterKey);

  if (prevKeyRef.current !== filterKey) {
    prevKeyRef.current = filterKey;
    extraPagesRef.current = 0;
  }

  useEffect(() => {
    const allow = shouldFetchMore({
      visibleCount,
      minVisible,
      hasNextPage,
      isFetching,
      extraPagesFetched: extraPagesRef.current,
      maxExtraPages: maxPages,
      enabled: Boolean(enabled && filterKey != null),
    });
    if (!allow) {
      return;
    }
    extraPagesRef.current += 1;
    fetchNextPage();
  }, [
    visibleCount,
    hasNextPage,
    isFetching,
    fetchNextPage,
    minVisible,
    maxPages,
    filterKey,
    enabled,
  ]);
}
