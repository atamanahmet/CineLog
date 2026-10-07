/**
 * Discover destinations for the navbar Discover menu and mobile Sheet.
 */
export const DISCOVER_NAV_ITEMS = [
  { id: "popular", label: "Popular", path: "/", exact: true },
  { id: "top-rated", label: "Top rated", path: "/top" },
  { id: "new-releases", label: "New releases", path: "/new" },
  { id: "upcoming", label: "Upcoming", path: "/upcoming" },
];

/** Single label for the Recommendation primary nav link. */
export const RECOMMENDATION_LABEL = "Recommendation";

/**
 * Auth-only primary nav links (profile tabs via ?tab=).
 */
export const PRIMARY_NAV_ITEMS = [
  {
    id: "recommendation",
    label: RECOMMENDATION_LABEL,
    path: "/profile",
    tab: "Recommendation",
    requiresAuth: true,
  },
  {
    id: "find-similar",
    label: "Find similar",
    path: "/profile",
    tab: "Find similar",
    requiresAuth: true,
  },
];

/** Flat list: discover then primary. */
export const NAV_ITEMS = [...DISCOVER_NAV_ITEMS, ...PRIMARY_NAV_ITEMS];

/**
 * Href for a nav item, including profile tab query when set.
 */
export function navItemHref(item) {
  if (item.tab != null) {
    return `${item.path}?tab=${encodeURIComponent(item.tab)}`;
  }
  return item.path;
}
