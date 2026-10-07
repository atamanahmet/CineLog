/**
 * Sample bounds matching GET /api/discover/defaults for tests.
 */
export const TEST_BOUNDS = {
  minVotesDefault: 100,
  voteCount: { min: 0, max: 10000 },
  runtime: { min: 0, max: 400 },
  yearRange: { min: 1800, max: 2100 },
  rating: { min: 0, max: 10 },
  maxPage: 500,
};
