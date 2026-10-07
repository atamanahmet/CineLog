/**
 * Empty-results copy for the find-similar grid.
 */
export function similarEmptyMessage(seedCount) {
  if (seedCount === 1) {
    return "We don't have enough data on this title yet. Try another one.";
  }
  return "We couldn't find close matches for these titles yet.";
}
