import { describe, expect, it } from "vitest";
import {
  applyTriStateFilter,
  cycleFilterValue,
  emptyTriState,
  filterKey,
  sanitizeTriState,
} from "./triStateFilter";

describe("cycleFilterValue", () => {
  it("cycles off → include → exclude → off", () => {
    const id = 28;
    const off = emptyTriState();
    const included = cycleFilterValue(off, id);
    expect(included).toEqual({ include: [28], exclude: [] });

    const excluded = cycleFilterValue(included, id);
    expect(excluded).toEqual({ include: [], exclude: [28] });

    const cleared = cycleFilterValue(excluded, id);
    expect(cleared).toEqual({ include: [], exclude: [] });
  });

  it("keeps other ids when cycling one", () => {
    const start = { include: [12], exclude: [16] };
    const next = cycleFilterValue(start, 12);
    expect(next).toEqual({ include: [], exclude: [16, 12] });
    expect(cycleFilterValue(next, 12)).toEqual({ include: [], exclude: [16] });
  });

  it("cycles string values", () => {
    const off = emptyTriState();
    const included = cycleFilterValue(off, "en");
    expect(included).toEqual({ include: ["en"], exclude: [] });
    expect(cycleFilterValue(included, "en")).toEqual({
      include: [],
      exclude: ["en"],
    });
  });
});

describe("filterKey", () => {
  it("is order-independent for include and exclude", () => {
    const a = { include: [28, 12], exclude: [16, 35] };
    const b = { include: [12, 28], exclude: [35, 16] };
    expect(filterKey(a)).toBe(filterKey(b));
    expect(filterKey(a)).toBe("12,28|16,35");
  });

  it("differs when selection differs", () => {
    expect(filterKey({ include: [28], exclude: [] })).not.toBe(
      filterKey({ include: [], exclude: [28] }),
    );
  });

  it("keys string values", () => {
    expect(filterKey({ include: ["tr", "en"], exclude: [] })).toBe("en,tr|");
  });
});

describe("sanitizeTriState", () => {
  it("keeps strings and positive integers", () => {
    expect(
      sanitizeTriState({ include: ["en", 28, "28", ""], exclude: ["tr"] }),
    ).toEqual({ include: ["en", 28], exclude: ["tr"] });
  });
});

describe("applyTriStateFilter", () => {
  const byGenre = (item) => item.genreIds ?? [];
  const byLang = (item) =>
    item.originalLanguage ? [item.originalLanguage] : [];

  it("include all requires every value", () => {
    const items = [
      { id: 1, genreIds: [878, 28] },
      { id: 2, genreIds: [878] },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: [878, 28], exclude: [] },
        byGenre,
        "all",
      ).map((i) => i.id),
    ).toEqual([1]);
  });

  it("include any keeps a partial match", () => {
    const items = [
      { id: 1, originalLanguage: "en" },
      { id: 2, originalLanguage: "tr" },
      { id: 3, originalLanguage: "ja" },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: ["en", "tr"], exclude: [] },
        byLang,
        "any",
      ).map((i) => i.id),
    ).toEqual([1, 2]);
  });

  it("exclude drops any match", () => {
    const items = [
      { id: 1, genreIds: [16] },
      { id: 2, genreIds: [28] },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: [], exclude: [16] },
        byGenre,
        "all",
      ).map((i) => i.id),
    ).toEqual([2]);
  });

  it("no values: pass exclude, fail include", () => {
    const bare = { id: 1, genreIds: [] };
    expect(
      applyTriStateFilter([bare], { include: [], exclude: [16] }, byGenre, "all"),
    ).toEqual([bare]);
    expect(
      applyTriStateFilter([bare], { include: [28], exclude: [] }, byGenre, "all"),
    ).toEqual([]);
  });

  it("include and exclude together", () => {
    const items = [
      { id: 1, genreIds: [878, 16] },
      { id: 2, genreIds: [878] },
      { id: 3, genreIds: [28] },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: [878], exclude: [16] },
        byGenre,
        "all",
      ).map((i) => i.id),
    ).toEqual([2]);
  });
});
