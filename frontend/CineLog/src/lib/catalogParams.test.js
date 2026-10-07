import { describe, expect, it } from "vitest";
import { buildCatalogParams, buildCatalogQueryKey } from "./catalogParams";
import {
  emptyFilters,
  migrateFilters,
  resolveEffectiveFilters,
  sanitizeFilters,
  showsRuntimeFilter,
  showsVoteCountFilter,
} from "./sanitizeFilters";
import {
  getPresetFilters,
  getPresetSort,
  getPresetUpcoming,
  pageKeyFromPath,
  pagePresets,
} from "./pagePresets";
import { TEST_BOUNDS } from "./testBounds";

describe("buildCatalogParams", () => {
  it("omits null voteCount and runtime", () => {
    const params = buildCatalogParams(1, {
      ...emptyFilters(TEST_BOUNDS),
      voteCount: null,
      minRuntime: null,
      maxRuntime: null,
    });
    expect(params.voteCount).toBeUndefined();
    expect(params.minRuntime).toBeUndefined();
    expect(params.maxRuntime).toBeUndefined();
  });

  it("includes set voteCount and runtime", () => {
    const params = buildCatalogParams(1, {
      ...emptyFilters(TEST_BOUNDS),
      voteCount: 1000,
      minRuntime: 60,
      maxRuntime: 120,
    });
    expect(params.voteCount).toBe(1000);
    expect(params.minRuntime).toBe(60);
    expect(params.maxRuntime).toBe(120);
  });

  it("maps genreFilter include and exclude", () => {
    const params = buildCatalogParams(1, {
      ...emptyFilters(TEST_BOUNDS),
      genreFilter: { include: [878, 28], exclude: [16, 35] },
    });
    expect(params.genreIdList).toBe("878,28");
    expect(params.withoutGenres).toBe("16,35");
  });

  it("omits withoutGenres when exclude empty", () => {
    const params = buildCatalogParams(1, {
      ...emptyFilters(TEST_BOUNDS),
      genreFilter: { include: [28], exclude: [] },
    });
    expect(params.genreIdList).toBe("28");
    expect(params).not.toHaveProperty("withoutGenres");
  });

  it("upcoming preset sends the flag and no yearRange", () => {
    const params = buildCatalogParams(
      1,
      { ...emptyFilters(TEST_BOUNDS), yearRange: [1900, 2040], voteCount: 0 },
      { upcoming: true, sort: "popularity.desc" },
    );
    expect(params.upcoming).toBe(true);
    expect(params).not.toHaveProperty("yearRange");
    expect(params.sort).toBe("popularity.desc");
  });

  it("other presets do not send the upcoming flag", () => {
    const params = buildCatalogParams(1, {
      ...emptyFilters(TEST_BOUNDS),
      yearRange: [1900, 2040],
    });
    expect(params).not.toHaveProperty("upcoming");
    expect(params.yearRange).toEqual([1900, 2040]);
  });

  it("query key differs between Popular and Upcoming", () => {
    const filters = emptyFilters(TEST_BOUNDS);
    const popular = buildCatalogQueryKey("discover", "movie", filters, {});
    const upcoming = buildCatalogQueryKey("upcoming", "movie", filters, {
      upcoming: true,
      sort: "popularity.desc",
    });
    expect(popular).not.toEqual(upcoming);
  });

  it("query key material changes when genreFilterKey changes", () => {
    const a = {
      ...emptyFilters(TEST_BOUNDS),
      genreKey: "878|16",
    };
    const b = {
      ...emptyFilters(TEST_BOUNDS),
      genreKey: "878|",
    };
    expect(JSON.stringify(["discover", "movie", a])).not.toEqual(
      JSON.stringify(["discover", "movie", b]),
    );
  });

  it("query key material changes when runtime changes", () => {
    const a = { ...emptyFilters(TEST_BOUNDS), minRuntime: 60, maxRuntime: 120 };
    const b = { ...emptyFilters(TEST_BOUNDS) };
    expect(JSON.stringify(["discover", "movie", a])).not.toEqual(
      JSON.stringify(["discover", "movie", b]),
    );
  });
});

describe("sanitizeFilters", () => {
  it("keeps null voteCount and runtime as null", () => {
    const result = sanitizeFilters(
      { ...emptyFilters(TEST_BOUNDS), voteCount: null, minRuntime: null },
      TEST_BOUNDS,
    );
    expect(result.voteCount).toBeNull();
    expect(result.minRuntime).toBeNull();
    expect(result.maxRuntime).toBeNull();
  });

  it("clamps set values to backend bounds", () => {
    const result = sanitizeFilters(
      {
        ...emptyFilters(TEST_BOUNDS),
        voteCount: 99999,
        minRuntime: -5,
        maxRuntime: 900,
        yearRange: [1000, 3000],
      },
      TEST_BOUNDS,
    );
    expect(result.voteCount).toBe(10000);
    expect(result.minRuntime).toBe(0);
    expect(result.maxRuntime).toBe(400);
    expect(result.yearRange).toEqual([1800, 2100]);
  });

  it("turns bad voteCount and runtime into null", () => {
    const result = sanitizeFilters(
      {
        ...emptyFilters(TEST_BOUNDS),
        voteCount: "x",
        minRuntime: "bad",
        maxRuntime: {},
      },
      TEST_BOUNDS,
    );
    expect(result.voteCount).toBeNull();
    expect(result.minRuntime).toBeNull();
    expect(result.maxRuntime).toBeNull();
  });
});

describe("migrateFilters", () => {
  it("turns saved voteCount 500 into null", () => {
    const migrated = migrateFilters(
      { voteCount: 500, duration: [60, 180] },
      3,
    );
    expect(migrated.voteCount).toBeNull();
    expect(migrated.duration).toBeUndefined();
    expect(migrated.minRuntime).toBeNull();
    expect(migrated.maxRuntime).toBeNull();
  });

  it("turns invalid voteCount into null", () => {
    expect(migrateFilters({ voteCount: "nope" }, 3).voteCount).toBeNull();
  });
});

describe("pagePresets", () => {
  it("maps paths to page keys", () => {
    expect(pageKeyFromPath("/")).toBe("discover");
    expect(pageKeyFromPath("/new")).toBe("newReleases");
    expect(pageKeyFromPath("/top")).toBe("top");
    expect(pageKeyFromPath("/upcoming")).toBe("upcoming");
    expect(pageKeyFromPath("/search")).toBe("search");
    expect(pageKeyFromPath("/profile")).toBe("watchlist");
  });

  it("Upcoming preset sets popularity sort and upcoming flag", () => {
    expect(getPresetSort("upcoming")).toBe("popularity.desc");
    expect(getPresetUpcoming("upcoming")).toBe(true);
    expect(getPresetUpcoming("discover")).toBe(false);
    expect(getPresetFilters("upcoming", "movie").voteCount).toBe(0);
  });

  it("Top preset vote floors and rating sort per media type", () => {
    expect(getPresetSort("top")).toBe("vote_average.desc");
    expect(getPresetFilters("top", "movie").voteCount).toBe(1000);
    expect(getPresetFilters("top", "tv").voteCount).toBe(1000);
    expect(getPresetFilters("discover", "movie")).toEqual({});
    expect(pagePresets.newReleases.filters).toEqual({});
  });

  it("resolveEffectiveFilters sends Top voteCount when user has null", () => {
    const effective = resolveEffectiveFilters(
      emptyFilters(TEST_BOUNDS),
      "top",
      "movie",
      TEST_BOUNDS,
    );
    expect(effective.voteCount).toBe(1000);
  });

  it("resolveEffectiveFilters keeps user vote override", () => {
    const effective = resolveEffectiveFilters(
      { ...emptyFilters(TEST_BOUNDS), voteCount: 200 },
      "top",
      "tv",
      TEST_BOUNDS,
    );
    expect(effective.voteCount).toBe(200);
  });
});

describe("showsRuntimeFilter", () => {
  it("shows on discover routes only", () => {
    expect(showsRuntimeFilter("/")).toBe(true);
    expect(showsRuntimeFilter("/new")).toBe(true);
    expect(showsRuntimeFilter("/top")).toBe(true);
    expect(showsRuntimeFilter("/upcoming")).toBe(true);
    expect(showsRuntimeFilter("/search")).toBe(false);
    expect(showsRuntimeFilter("/profile")).toBe(false);
  });

  it("vote count uses the same discover routes", () => {
    expect(showsVoteCountFilter("/")).toBe(true);
    expect(showsVoteCountFilter("/upcoming")).toBe(true);
    expect(showsVoteCountFilter("/search")).toBe(false);
  });
});
