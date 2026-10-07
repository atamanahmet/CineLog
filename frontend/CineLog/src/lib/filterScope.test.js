import { describe, expect, it } from "vitest";
import { filterScopeKey, YEAR_FILTER_SCOPES } from "./filterScope";

const PROFILE_TABS = [
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Recommendation",
  "Find similar",
];

describe("filterScopeKey", () => {
  it("maps catalog paths", () => {
    expect(filterScopeKey("/")).toBe("discover");
    expect(filterScopeKey("/new")).toBe("new");
    expect(filterScopeKey("/top")).toBe("top");
    expect(filterScopeKey("/upcoming")).toBe("upcoming");
    expect(filterScopeKey("/search")).toBe("search");
  });

  it("maps each profile tab id", () => {
    for (const tab of PROFILE_TABS) {
      expect(filterScopeKey("/profile", tab)).toBe(tab);
    }
  });

  it("returns null for unknown paths", () => {
    expect(filterScopeKey("/settings")).toBeNull();
    expect(filterScopeKey("/details/movie/1")).toBeNull();
    expect(filterScopeKey("/person/1")).toBeNull();
    expect(filterScopeKey("/actor/1")).toBeNull();
  });

  it("strips trailing slashes", () => {
    expect(filterScopeKey("/new/")).toBe("new");
    expect(filterScopeKey("/profile/", "Loved")).toBe("Loved");
  });
});

describe("YEAR_FILTER_SCOPES", () => {
  it("upcoming has no year section, discover still has it", () => {
    expect(YEAR_FILTER_SCOPES.has("upcoming")).toBe(false);
    expect(YEAR_FILTER_SCOPES.has("discover")).toBe(true);
  });
});
