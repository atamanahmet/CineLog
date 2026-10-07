import { describe, expect, it } from "vitest";
import { countActiveFilters } from "./countActiveFilters";
import { emptyTriState } from "./triStateFilter";

const defaults = {
  genreFilter: emptyTriState(),
  languageFilter: emptyTriState(),
  yearRange: [1900, 2026],
  rating: [0, 10],
  languages: [],
  adult: false,
  voteCount: null,
  minRuntime: null,
  maxRuntime: null,
};

const scopeWithLanguages = { languages: true };
const scopeWithoutLanguages = { languages: false };

describe("countActiveFilters", () => {
  it("returns zero at defaults", () => {
    expect(countActiveFilters(defaults, defaults, scopeWithLanguages)).toBe(0);
  });

  it("counts genre include and exclude separately", () => {
    const filters = {
      ...defaults,
      genreFilter: { include: [28, 12], exclude: [27] },
    };
    expect(countActiveFilters(filters, defaults, scopeWithLanguages)).toBe(3);
  });

  it("counts year range once when it differs from default", () => {
    const filters = {
      ...defaults,
      yearRange: [2000, 2020],
    };
    expect(countActiveFilters(filters, defaults, scopeWithLanguages)).toBe(1);
  });

  it("does not count year for upcoming scope", () => {
    const filters = {
      ...defaults,
      yearRange: [2000, 2020],
    };
    expect(
      countActiveFilters(filters, defaults, {
        languages: true,
        yearRange: false,
      }),
    ).toBe(0);
  });

  it("ignores language for a scope without languages", () => {
    const filters = {
      ...defaults,
      languageFilter: { include: ["en"], exclude: ["tr"] },
      genreFilter: { include: [28], exclude: [] },
    };
    expect(countActiveFilters(filters, defaults, scopeWithoutLanguages)).toBe(1);
  });

  it("ignores sort and media type", () => {
    const filters = {
      ...defaults,
      sort: "vote_average.desc",
      mediaType: "tv",
      genreFilter: { include: [28], exclude: [] },
    };
    expect(countActiveFilters(filters, defaults, scopeWithLanguages)).toBe(1);
  });
});
