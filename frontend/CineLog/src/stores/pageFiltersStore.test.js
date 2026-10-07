import { beforeEach, describe, expect, it } from "vitest";
import { PAGE_MEMORY_TTL_MS } from "../lib/pageMemory";
import { usePageFiltersStore } from "./pageFiltersStore";
import { useUserPrefsStore } from "./userPrefsStore";

const sampleFilters = {
  genreFilter: { include: [28], exclude: [16] },
  yearRange: [2000, 2020],
  rating: [0, 10],
  languages: ["en"],
  languageFilter: { include: ["en"], exclude: ["tr"] },
  adult: false,
  voteCount: 200,
  minRuntime: null,
  maxRuntime: null,
};

beforeEach(() => {
  usePageFiltersStore.getState().clearAll();
});

describe("pageFiltersStore TTL", () => {
  it("keeps an entry when return is within TTL", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("discover", sampleFilters, "movie");
    store.markLeft("discover", 1000);
    store.markEntered("discover", 1000 + PAGE_MEMORY_TTL_MS - 1);
    expect(store.getFilters("discover", "movie")).toEqual(sampleFilters);
  });

  it("drops an entry when return is past TTL", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("discover", sampleFilters, "movie");
    store.markLeft("discover", 1000);
    store.markEntered("discover", 1000 + PAGE_MEMORY_TTL_MS + 1);
    expect(usePageFiltersStore.getState().getFilters("discover", "movie")).toBeNull();
  });

  it("keeps an entry that was never left", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("discover", sampleFilters, "movie");
    store.markEntered("discover", 999999);
    expect(store.getFilters("discover", "movie")).toEqual(sampleFilters);
  });

  it("resetFilters deletes the entry", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("top", sampleFilters, "movie");
    store.resetFilters("top");
    expect(store.getFilters("top", "movie")).toBeNull();
  });

  it("clearAll empties everything", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("discover", sampleFilters, "movie");
    store.setFilters("top", sampleFilters, "tv");
    store.clearAll();
    expect(usePageFiltersStore.getState().entries).toEqual({});
  });
});

describe("pageFiltersStore media type", () => {
  it("clears genreFilter but keeps languageFilter when media type changes", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("discover", sampleFilters, "movie");
    const next = store.getFilters("discover", "tv");
    expect(next.genreFilter).toEqual({ include: [], exclude: [] });
    expect(next.languageFilter).toEqual(sampleFilters.languageFilter);
    expect(next.yearRange).toEqual(sampleFilters.yearRange);
    expect(next.rating).toEqual(sampleFilters.rating);
    expect(next.languages).toEqual(sampleFilters.languages);
    expect(next.voteCount).toBe(sampleFilters.voteCount);
  });
});

describe("pageFiltersStore isolation", () => {
  it("recommendation filters are not visible in discover", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters("Recommendation", sampleFilters, "movie");
    expect(store.getFilters("discover", "movie")).toBeNull();
    expect(store.getFilters("Recommendation", "movie")).toEqual(sampleFilters);
  });
});

describe("pageFiltersStore applyGenreEntry", () => {
  it("sets media type, one included genre, clears exclude, keeps language", () => {
    const store = usePageFiltersStore.getState();
    store.setFilters(
      "discover",
      {
        ...sampleFilters,
        genreFilter: { include: [28, 12], exclude: [16] },
      },
      "movie",
    );
    useUserPrefsStore.setState({ mediaType: "movie" });

    store.applyGenreEntry("tv", 16);

    expect(useUserPrefsStore.getState().mediaType).toBe("tv");
    const next = store.getFilters("discover", "tv");
    expect(next.genreFilter).toEqual({ include: [16], exclude: [] });
    expect(next.languageFilter).toEqual(sampleFilters.languageFilter);
    expect(next.yearRange).toEqual(sampleFilters.yearRange);
    expect(next.rating).toEqual(sampleFilters.rating);
    expect(next.voteCount).toBe(sampleFilters.voteCount);
  });
});
