/**
 * Build catalog/browse query params from page + shared filters.
 * Pass extras for page-specific keys (query, releaseWindow, compare, upcoming, …).
 * Extras win on key collision. Null voteCount and runtime are omitted.
 * Upcoming requests omit yearRange.
 */
export function buildCatalogParams(page, filters = {}, extras = {}) {
  const upcoming = Boolean(extras.upcoming);
  const params = {};
  if (page != null) {
    params.page = page;
  }
  if ("adult" in filters) {
    params.adult = filters.adult;
  }
  const include = filters.genreFilter?.include ?? [];
  if (include.length) {
    params.genreIdList = include.join(",");
  }
  const exclude = filters.genreFilter?.exclude ?? [];
  if (exclude.length) {
    params.withoutGenres = exclude.join(",");
  }
  if (!upcoming && filters.yearRange?.length === 2) {
    params.yearRange = filters.yearRange;
  }
  if (filters.rating?.length === 2) {
    params.ratingRange = filters.rating;
  }
  if (filters.languages?.length) {
    params.languages = filters.languages.join(",");
  }
  if (filters.voteCount != null && Number.isFinite(Number(filters.voteCount))) {
    params.voteCount = Number(filters.voteCount);
  }
  if (filters.minRuntime != null && Number.isFinite(Number(filters.minRuntime))) {
    params.minRuntime = Number(filters.minRuntime);
  }
  if (filters.maxRuntime != null && Number.isFinite(Number(filters.maxRuntime))) {
    params.maxRuntime = Number(filters.maxRuntime);
  }
  if (filters.sort) {
    params.sort = filters.sort;
  }
  return { ...params, ...extras };
}

/**
 * React Query key prefix for a catalog infinite query.
 */
export function buildCatalogQueryKey(pageKey, mediaType, filters, extras = {}) {
  return [pageKey, mediaType, filters, extras];
}
