/**
 * True when voteAverage is a real rating (not missing or zero).
 */
export function hasRating(voteAverage) {
  return voteAverage != null && voteAverage !== 0;
}
