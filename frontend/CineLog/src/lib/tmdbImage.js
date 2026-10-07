const SIZE_BUCKETS = {
  poster: ["w92", "w154", "w185", "w342", "w500", "w780"],
  backdrop: ["w780", "w1280", "original"],
  profile: ["w45", "w185", "h632"],
};

/**
 * One TMDB image URL for a known size.
 */
export function getTmdbImageUrl(path, type, size) {
  if (!SIZE_BUCKETS[type]?.includes(size)) {
    throw new Error(`Invalid TMDB image size "${size}" for type "${type}"`);
  }
  return `https://image.tmdb.org/t/p/${size}${path}`;
}

/**
 * Srcset for a size bucket. Original counts as 1920w.
 */
export function getTmdbSrcSet(path, type) {
  return SIZE_BUCKETS[type]
    .filter((size) => size.startsWith("w") || size === "original")
    .map((size) => {
      const n = size === "original" ? "1920" : size.slice(1);
      return `${getTmdbImageUrl(path, type, size)} ${n}w`;
    })
    .join(", ");
}

/**
 * Backdrop srcset for the screen width. Empty path returns nothing.
 */
export function backdropSrcSet(path) {
  if (path == null || path === "") {
    return "";
  }
  return getTmdbSrcSet(path, "backdrop");
}
