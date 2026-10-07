import { describe, expect, it } from "vitest";
import {
  DISCOVER_NAV_ITEMS,
  NAV_ITEMS,
  PRIMARY_NAV_ITEMS,
  RECOMMENDATION_LABEL,
  navItemHref,
} from "./navItems";

describe("navItems", () => {
  it("keeps Recommendation label as one constant", () => {
    expect(RECOMMENDATION_LABEL).toBe("Recommendation");
    expect(PRIMARY_NAV_ITEMS[0].label).toBe(RECOMMENDATION_LABEL);
  });

  it("lists discover destinations", () => {
    expect(DISCOVER_NAV_ITEMS.map((item) => [item.label, item.path])).toEqual([
      ["Popular", "/"],
      ["Top rated", "/top"],
      ["New releases", "/new"],
      ["Upcoming", "/upcoming"],
    ]);
  });

  it("builds profile tab hrefs", () => {
    expect(navItemHref(PRIMARY_NAV_ITEMS[0])).toBe(
      "/profile?tab=Recommendation",
    );
    expect(navItemHref(PRIMARY_NAV_ITEMS[1])).toBe(
      "/profile?tab=Find%20similar",
    );
    expect(navItemHref(DISCOVER_NAV_ITEMS[0])).toBe("/");
  });

  it("marks primary items as auth-only", () => {
    expect(PRIMARY_NAV_ITEMS.every((item) => item.requiresAuth)).toBe(true);
    expect(NAV_ITEMS).toHaveLength(
      DISCOVER_NAV_ITEMS.length + PRIMARY_NAV_ITEMS.length,
    );
  });
});
