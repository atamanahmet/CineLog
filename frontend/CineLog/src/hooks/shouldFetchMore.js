/**
 * Pure gate for filling a filtered list by fetching more pages.
 * Cap is hard: extraPagesFetched must stay under maxExtraPages.
 */
export function shouldFetchMore({
  visibleCount,
  minVisible,
  hasNextPage,
  isFetching,
  extraPagesFetched,
  maxExtraPages,
  enabled = true,
}) {
  if (!enabled) {
    return false;
  }
  if (isFetching) {
    return false;
  }
  if (!hasNextPage) {
    return false;
  }
  if (visibleCount >= minVisible) {
    return false;
  }
  if (extraPagesFetched >= maxExtraPages) {
    return false;
  }
  return true;
}
