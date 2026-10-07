import { describe, expect, it } from "vitest";
import {
  GENRE_INCLUDE_MODE,
  TV_EXPAND,
  toCanonicalGenreIds,
} from "./genreCanonical";
import { applyTriStateFilter } from "./triStateFilter";

describe("toCanonicalGenreIds", () => {
  it("maps every TV_EXPAND row", () => {
    for (const [raw, expected] of Object.entries(TV_EXPAND)) {
      const id = Number(raw);
      expect(
        toCanonicalGenreIds({ mediaType: "TV", genreIds: [id] }),
      ).toEqual([...expected]);
    }
  });

  it("maps Action & Adventure 10759 to Action and Adventure", () => {
    expect(
      toCanonicalGenreIds({ mediaType: "TV", genreIds: [10759] }),
    ).toEqual([28, 12]);
  });

  it("keeps a plain movie id", () => {
    expect(
      toCanonicalGenreIds({ mediaType: "MOVIE", genreIds: [28] }),
    ).toEqual([28]);
  });

  it("keeps a plain TV self id", () => {
    expect(
      toCanonicalGenreIds({ mediaType: "TV", genreIds: [16] }),
    ).toEqual([16]);
  });

  it("dedupes after expand", () => {
    expect(
      toCanonicalGenreIds({
        mediaType: "TV",
        genreIds: [10759, 28, 12],
      }),
    ).toEqual([28, 12]);
  });

  it("returns empty when genre ids missing", () => {
    expect(toCanonicalGenreIds({ mediaType: "MOVIE" })).toEqual([]);
    expect(toCanonicalGenreIds({ mediaType: "TV", genreIds: [] })).toEqual([]);
  });
});

describe("genre applyTriStateFilter", () => {
  it("reports include mode all (engine constant)", () => {
    expect(GENRE_INCLUDE_MODE).toBe("all");
  });

  it("exclude drops any match", () => {
    const items = [
      { id: 1, mediaType: "MOVIE", genreIds: [16] },
      { id: 2, mediaType: "MOVIE", genreIds: [28] },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: [], exclude: [16] },
        toCanonicalGenreIds,
        GENRE_INCLUDE_MODE,
      ).map((i) => i.id),
    ).toEqual([2]);
  });

  it("include ALL requires every id", () => {
    const items = [
      { id: 1, mediaType: "MOVIE", genreIds: [878, 28] },
      { id: 2, mediaType: "MOVIE", genreIds: [878] },
    ];
    expect(
      applyTriStateFilter(
        items,
        { include: [878, 28], exclude: [] },
        toCanonicalGenreIds,
        GENRE_INCLUDE_MODE,
      ).map((i) => i.id),
    ).toEqual([1]);
  });

  it("TV Action & Adventure expands for exclude Action", () => {
    const show = { id: 9, mediaType: "TV", genreIds: [10759] };
    expect(
      applyTriStateFilter(
        [show],
        { include: [], exclude: [28] },
        toCanonicalGenreIds,
        GENRE_INCLUDE_MODE,
      ),
    ).toEqual([]);
  });
});
