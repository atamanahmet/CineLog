/** Cap skeleton placeholders so a high maxResults does not flood the grid. */
export const RECOMMENDATION_SKELETON_MAX = 24;

/**
 * Skeleton count for recommendation grids.
 */
export function recommendationSkeletonCount(maxResults) {
  return Math.min(maxResults, RECOMMENDATION_SKELETON_MAX);
}
