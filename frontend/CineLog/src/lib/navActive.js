/**
 * True when this nav item matches the current location.
 * Exact path items match pathname only. Tab items need pathname and tab.
 */
export function isNavItemActive(item, location) {
  const pathname = (location?.pathname ?? "").replace(/\/+$/, "") || "/";
  const itemPath = (item.path ?? "").replace(/\/+$/, "") || "/";

  if (item.tab != null) {
    const raw = location?.search ?? "";
    const params = new URLSearchParams(
      raw.startsWith("?") ? raw.slice(1) : raw,
    );
    return pathname === itemPath && params.get("tab") === item.tab;
  }

  if (item.exact) {
    return pathname === itemPath;
  }
  return pathname === itemPath;
}

/**
 * Filter nav items for the current auth state.
 */
export function getVisibleNavItems(items, isAuthenticated) {
  return items.filter((item) => !item.requiresAuth || isAuthenticated);
}
