/**
 * Map GET /api/discover/defaults JSON into filter bounds used by sanitize and UI.
 */
export function mapDiscoverDefaults(api) {
  if (api == null || typeof api !== "object") {
    throw new Error("Discover defaults payload missing");
  }
  return {
    minVotesDefault: api.minVotes.default,
    voteCount: { min: 0, max: api.minVotes.max },
    runtime: { min: api.runtime.min, max: api.runtime.max },
    yearRange: { min: api.year.min, max: api.year.max },
    rating: { min: api.rating.min, max: api.rating.max },
    maxPage: api.maxPage,
  };
}
