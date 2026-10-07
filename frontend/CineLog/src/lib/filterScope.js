/**
 * Map a route and optional profile tab to a filter memory scope key.
 */
export function filterScopeKey(pathname, profileTab) {
  const path = (pathname ?? "").replace(/\/+$/, "") || "/";
  if (path === "/") {
    return "discover";
  }
  if (path === "/new") {
    return "new";
  }
  if (path === "/top") {
    return "top";
  }
  if (path === "/upcoming") {
    return "upcoming";
  }
  if (path === "/search") {
    return "search";
  }
  if (path === "/profile") {
    return profileTab ?? null;
  }
  return null;
}

/**
 * Scopes that show the Year filter section. Upcoming is omitted on purpose.
 */
export const YEAR_FILTER_SCOPES = new Set([
  "discover",
  "new",
  "top",
  "search",
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
  "Recommendation",
  "Find similar",
]);

