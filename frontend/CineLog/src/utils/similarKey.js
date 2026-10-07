/**
 * Stable cache key from seeds. Order of seeds does not change the key.
 */
export function similarKey(seeds) {
  return (seeds ?? [])
    .map((seed) => `${seed.mediaType}:${seed.tmdbId}`)
    .sort()
    .join("|");
}
