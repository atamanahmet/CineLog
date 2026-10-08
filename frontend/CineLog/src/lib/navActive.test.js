import { describe, expect, it } from "vitest";
import {
  DISCOVER_NAV_ITEMS,
  NAV_ITEMS,
  PRIMARY_NAV_ITEMS,
  RECOMMENDATION_LABEL,
} from "./navItems";
import { getVisibleNavItems, isNavItemActive } from "./navActive";

describe("navItems", () => {
  it("keeps Recommendation label as one constant", () => {
    expect(RECOMMENDATION_LABEL).toBe("Recommendation");
    expect(PRIMARY_NAV_ITEMS[0].label).toBe(RECOMMENDATION_LABEL);
  });

  it("lists discover and primary groups", () => {
    expect(DISCOVER_NAV_ITEMS.map((item) => item.path)).toEqual([
      "/",
      "/top",
      "/new",
      "/upcoming",
    ]);
    expect(PRIMARY_NAV_ITEMS.every((item) => item.requiresAuth)).toBe(true);
    expect(NAV_ITEMS).toHaveLength(
      DISCOVER_NAV_ITEMS.length + PRIMARY_NAV_ITEMS.length,
    );
  });
});

describe("isNavItemActive", () => {
  const popular = DISCOVER_NAV_ITEMS[0];
  const top = DISCOVER_NAV_ITEMS[1];
  const recommendation = PRIMARY_NAV_ITEMS[0];
  const findSimilar = PRIMARY_NAV_ITEMS[1];

  it("matches / exact only", () => {
    expect(isNavItemActive(popular, { pathname: "/", search: "" })).toBe(true);
    expect(isNavItemActive(popular, { pathname: "/new", search: "" })).toBe(
      false,
    );
  });

  it("matches path items by pathname", () => {
    expect(isNavItemActive(top, { pathname: "/top", search: "" })).toBe(true);
    expect(isNavItemActive(top, { pathname: "/new", search: "" })).toBe(false);
  });

  it("matches tab items by pathname and tab", () => {
    expect(
      isNavItemActive(recommendation, {
        pathname: "/profile",
        search: "?tab=Recommendation",
      }),
    ).toBe(true);
    expect(
      isNavItemActive(recommendation, {
        pathname: "/profile",
        search: "?tab=Find%20similar",
      }),
    ).toBe(false);
    expect(
      isNavItemActive(findSimilar, {
        pathname: "/profile",
        search: "?tab=Find%20similar",
      }),
    ).toBe(true);
    expect(
      isNavItemActive(recommendation, { pathname: "/settings", search: "" }),
    ).toBe(false);
  });
});

describe("getVisibleNavItems", () => {
  it("hides auth items for guests", () => {
    const visible = getVisibleNavItems(NAV_ITEMS, false);
    expect(visible.every((item) => !item.requiresAuth)).toBe(true);
    expect(visible).toHaveLength(DISCOVER_NAV_ITEMS.length);
  });

  it("shows all items when logged in", () => {
    expect(getVisibleNavItems(NAV_ITEMS, true)).toEqual(NAV_ITEMS);
  });
});
