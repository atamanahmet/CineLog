import { describe, expect, it } from "vitest";
import { deriveFilteredSortedMedia } from "../hooks/useFilteredSortedMedia";
import { TEST_BOUNDS } from "../lib/testBounds";

const MOVIE_A = {
  id: 1,
  mediaType: "MOVIE",
  title: "Alpha",
  releaseDate: "2000-01-01",
  voteAverage: 5,
  score: 0.5,
};
const MOVIE_B = {
  id: 2,
  mediaType: "MOVIE",
  title: "Bravo",
  releaseDate: "2010-01-01",
  voteAverage: 9,
  score: 0.9,
};
const TV_A = {
  id: 3,
  mediaType: "TV",
  title: "Charlie",
  releaseDate: "2005-01-01",
  voteAverage: 8,
  score: 0.8,
};

const fullFilters = {
  genreFilter: { include: [], exclude: [] },
  languageFilter: { include: [], exclude: [] },
  yearRange: [TEST_BOUNDS.yearRange.min, TEST_BOUNDS.yearRange.max],
  rating: [TEST_BOUNDS.rating.min, TEST_BOUNDS.rating.max],
  languages: [],
  adult: false,
  voteCount: null,
  minRuntime: null,
  maxRuntime: null,
};

describe("deriveFilteredSortedMedia", () => {
  it("merges, then filters, then sorts", () => {
    const result = deriveFilteredSortedMedia({
      movies: [MOVIE_A, MOVIE_B],
      tv: [TV_A],
      view: "all",
      sortKey: "titleAsc",
      filters: { ...fullFilters, rating: [7, 10] },
      activeKeys: ["rating"],
      bounds: TEST_BOUNDS,
    });
    expect(result.total).toBe(3);
    expect(result.shown).toBe(2);
    expect(result.items.map((item) => item.title)).toEqual(["Bravo", "Charlie"]);
  });

  it("returns total and shown counts", () => {
    const result = deriveFilteredSortedMedia({
      movies: [MOVIE_A, MOVIE_B],
      tv: null,
      view: "movie",
      sortKey: "match",
      filters: { ...fullFilters, yearRange: [2005, 2100] },
      activeKeys: ["yearRange"],
      bounds: TEST_BOUNDS,
    });
    expect(result.total).toBe(2);
    expect(result.shown).toBe(1);
    expect(result.items).toEqual([MOVIE_B]);
  });
});
