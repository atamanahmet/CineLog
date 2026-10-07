import { describe, expect, it } from "vitest";
import { filterMediaItems } from "./media";
import { TEST_BOUNDS } from "../lib/testBounds";

const BASE = {
  id: 1,
  mediaType: "MOVIE",
  title: "Sample",
  originalTitle: "Sample",
  posterPath: null,
  releaseDate: "2000-06-15",
  voteAverage: 7,
  overview: null,
  adult: false,
  originalLanguage: "en",
  character: null,
  score: 0.9,
};

const ALL_KEYS = [
  "yearRange",
  "rating",
  "languages",
  "adult",
  "voteCount",
  "runtime",
];

function filters(partial = {}) {
  return {
    genreFilter: { include: [], exclude: [] },
    languageFilter: { include: [], exclude: [] },
    yearRange: [TEST_BOUNDS.yearRange.min, TEST_BOUNDS.yearRange.max],
    rating: [TEST_BOUNDS.rating.min, TEST_BOUNDS.rating.max],
    languages: [],
    adult: false,
    voteCount: null,
    minRuntime: null,
    maxRuntime: null,
    ...partial,
  };
}

describe("filterMediaItems", () => {
  it("yearRange includes and excludes", () => {
    const inRange = { ...BASE, releaseDate: "2000-01-01" };
    const out = { ...BASE, id: 2, releaseDate: "1990-01-01" };
    const state = filters({ yearRange: [1995, 2005] });
    const result = filterMediaItems(
      [inRange, out],
      state,
      ["yearRange"],
      TEST_BOUNDS,
    );
    expect(result).toEqual([inRange]);
  });

  it("rating includes and excludes", () => {
    const high = { ...BASE, voteAverage: 8 };
    const low = { ...BASE, id: 2, voteAverage: 3 };
    const state = filters({ rating: [6, 10] });
    const result = filterMediaItems([high, low], state, ["rating"], TEST_BOUNDS);
    expect(result).toEqual([high]);
  });

  it("languages includes and excludes", () => {
    const en = { ...BASE, originalLanguage: "en" };
    const tr = { ...BASE, id: 2, originalLanguage: "tr" };
    const state = filters({ languages: ["en"] });
    const result = filterMediaItems(
      [en, tr],
      state,
      ["languages"],
      TEST_BOUNDS,
    );
    expect(result).toEqual([en]);
  });

  it("adult includes and excludes", () => {
    const safe = { ...BASE, adult: false };
    const adultItem = { ...BASE, id: 2, adult: true };
    const hideAdult = filters({ adult: false });
    expect(
      filterMediaItems([safe, adultItem], hideAdult, ["adult"], TEST_BOUNDS),
    ).toEqual([safe]);
    const showAdult = filters({ adult: true });
    expect(
      filterMediaItems([safe, adultItem], showAdult, ["adult"], TEST_BOUNDS),
    ).toEqual([safe, adultItem]);
  });

  it("voteCount includes and excludes", () => {
    const popular = { ...BASE, voteCount: 2000 };
    const rare = { ...BASE, id: 2, voteCount: 50 };
    const state = filters({ voteCount: 500 });
    const result = filterMediaItems(
      [popular, rare],
      state,
      ["voteCount"],
      TEST_BOUNDS,
    );
    expect(result).toEqual([popular]);
  });

  it("runtime includes and excludes", () => {
    const mid = { ...BASE, runtime: 120 };
    const short = { ...BASE, id: 2, runtime: 40 };
    const state = filters({ minRuntime: 90, maxRuntime: 150 });
    const result = filterMediaItems(
      [mid, short],
      state,
      ["runtime"],
      TEST_BOUNDS,
    );
    expect(result).toEqual([mid]);
  });

  it("ignores a narrowed key that is not in activeKeys", () => {
    const low = { ...BASE, voteAverage: 2 };
    const state = filters({ rating: [8, 10], yearRange: [1995, 2005] });
    const result = filterMediaItems(
      [low],
      state,
      ["yearRange"],
      TEST_BOUNDS,
    );
    expect(result).toEqual([low]);
  });

  it("unknown values: full range passes, narrowed excludes", () => {
    const unknown = {
      ...BASE,
      releaseDate: null,
      voteAverage: null,
      originalLanguage: null,
      adult: null,
      genreIds: null,
      voteCount: null,
      runtime: null,
    };
    const full = filters();
    expect(
      filterMediaItems([unknown], full, ALL_KEYS, TEST_BOUNDS),
    ).toEqual([unknown]);

    expect(
      filterMediaItems(
        [unknown],
        filters({ yearRange: [1990, 2000] }),
        ["yearRange"],
        TEST_BOUNDS,
      ),
    ).toEqual([]);
    expect(
      filterMediaItems(
        [unknown],
        filters({ rating: [5, 10] }),
        ["rating"],
        TEST_BOUNDS,
      ),
    ).toEqual([]);
    expect(
      filterMediaItems(
        [unknown],
        filters({ languages: ["en"] }),
        ["languages"],
        TEST_BOUNDS,
      ),
    ).toEqual([]);
    expect(
      filterMediaItems(
        [unknown],
        filters({ voteCount: 100 }),
        ["voteCount"],
        TEST_BOUNDS,
      ),
    ).toEqual([]);
    expect(
      filterMediaItems(
        [unknown],
        filters({ minRuntime: 60, maxRuntime: 120 }),
        ["runtime"],
        TEST_BOUNDS,
      ),
    ).toEqual([]);
  });
});
