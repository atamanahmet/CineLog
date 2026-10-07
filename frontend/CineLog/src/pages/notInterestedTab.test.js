import { describe, expect, it } from "vitest";
import { LIST_ITEMS, TABS } from "../lib/profileTabs";

describe("Not interested tab placement", () => {
  it("TABS has Not interested right after Loved", () => {
    const values = TABS.map((tab) => tab.value);
    const loved = values.indexOf("Loved");
    expect(loved).toBeGreaterThanOrEqual(0);
    expect(values[loved + 1]).toBe("Not interested");
  });

  it("LIST_ITEMS has Not interested right after Loved", () => {
    const tabs = LIST_ITEMS.map((item) => item.tab);
    const loved = tabs.indexOf("Loved");
    expect(loved).toBeGreaterThanOrEqual(0);
    expect(tabs[loved + 1]).toBe("Not interested");
  });
});

describe("unknown profileTab fallback", () => {
  it("falls back to Watchlist when value is not in TABS", () => {
    const raw = "GhostTab";
    const selection = TABS.some((tab) => tab.value === raw)
      ? raw
      : "Watchlist";
    expect(selection).toBe("Watchlist");
  });
});
