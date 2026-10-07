import { describe, expect, it, vi } from "vitest";
import { normalizeMediaItem } from "./media";

describe("normalizeMediaItem", () => {
  it("maps movie raw TMDB shape", () => {
    const result = normalizeMediaItem({
      id: 99,
      title: "Heat",
      original_title: "Heat",
      poster_path: "/heat.jpg",
      release_date: "1995-12-15",
      vote_average: 7.9,
      overview: "Crime drama",
      media_type: "movie",
      adult: false,
      original_language: "en",
      popularity: 12.3,
    });

    expect(result).toEqual({
      id: 99,
      mediaType: "MOVIE",
      title: "Heat",
      originalTitle: "Heat",
      posterPath: "/heat.jpg",
      releaseDate: "1995-12-15",
      voteAverage: 7.9,
      overview: "Crime drama",
      adult: false,
      originalLanguage: "en",
      character: null,
      score: null,
      genreIds: [],
    });
  });

  it("maps TV raw TMDB shape", () => {
    const result = normalizeMediaItem({
      id: 88,
      name: "The Wire",
      original_name: "The Wire",
      poster_path: "/wire.jpg",
      first_air_date: "2002-06-02",
      vote_average: 8.5,
      overview: "Baltimore",
      media_type: "tv",
      adult: false,
      original_language: "en",
    });

    expect(result).toEqual({
      id: 88,
      mediaType: "TV",
      title: "The Wire",
      originalTitle: "The Wire",
      posterPath: "/wire.jpg",
      releaseDate: "2002-06-02",
      voteAverage: 8.5,
      overview: "Baltimore",
      adult: false,
      originalLanguage: "en",
      character: null,
      score: null,
      genreIds: [],
    });
  });

  it("keeps engine score as a canonical nullable field", () => {
    const result = normalizeMediaItem({
      id: 99,
      title: "Heat",
      original_title: "Heat",
      poster_path: "/heat.jpg",
      release_date: "1995-12-15",
      overview: "Crime drama",
      vote_average: 7.9,
      score: 0.91,
      media_type: "MOVIE",
    });

    expect(result).toEqual({
      id: 99,
      mediaType: "MOVIE",
      title: "Heat",
      originalTitle: "Heat",
      posterPath: "/heat.jpg",
      releaseDate: "1995-12-15",
      voteAverage: 7.9,
      overview: "Crime drama",
      adult: null,
      originalLanguage: null,
      character: null,
      score: 0.91,
      genreIds: [],
    });
  });

  it("maps backend camelCase shape", () => {
    const result = normalizeMediaItem({
      id: 42,
      title: "Dune",
      originalTitle: "Dune",
      posterPath: "/dune.jpg",
      releaseDate: "2021-10-22",
      overview: "Sand",
      voteAverage: 8.1,
      adult: false,
      originalLanguage: "en",
      mediaType: "movie",
      popularity: 99,
    });

    expect(result).toEqual({
      id: 42,
      mediaType: "MOVIE",
      title: "Dune",
      originalTitle: "Dune",
      posterPath: "/dune.jpg",
      releaseDate: "2021-10-22",
      voteAverage: 8.1,
      overview: "Sand",
      adult: false,
      originalLanguage: "en",
      character: null,
      score: null,
      genreIds: [],
    });
    expect(result).not.toHaveProperty("popularity");
  });

  it("maps genre_ids and genreIds onto genreIds", () => {
    expect(
      normalizeMediaItem({
        id: 1,
        title: "A",
        media_type: "movie",
        genre_ids: [28, 80],
      }).genreIds,
    ).toEqual([28, 80]);
    expect(
      normalizeMediaItem({
        id: 2,
        title: "B",
        mediaType: "movie",
        genreIds: [18],
      }).genreIds,
    ).toEqual([18]);
  });

  it("missing fields give null without throwing", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => normalizeMediaItem({})).not.toThrow();
    expect(normalizeMediaItem({})).toEqual({
      id: null,
      mediaType: "MOVIE",
      title: null,
      originalTitle: null,
      posterPath: null,
      releaseDate: null,
      voteAverage: null,
      overview: null,
      adult: null,
      originalLanguage: null,
      character: null,
      score: null,
      genreIds: [],
    });
    expect(normalizeMediaItem(null)).toEqual({
      id: null,
      mediaType: "MOVIE",
      title: null,
      originalTitle: null,
      posterPath: null,
      releaseDate: null,
      voteAverage: null,
      overview: null,
      adult: null,
      originalLanguage: null,
      character: null,
      score: null,
      genreIds: [],
    });
    spy.mockRestore();
  });

  it("drops extra fields but keeps score and genreIds", () => {
    const result = normalizeMediaItem({
      id: 1,
      title: "A",
      mediaType: "movie",
      backdropPath: "/b.jpg",
      genreIds: [1, 2],
      score: 0.5,
      popularity: 10,
    });

    expect(Object.keys(result).sort()).toEqual(
      [
        "adult",
        "character",
        "genreIds",
        "id",
        "mediaType",
        "originalLanguage",
        "originalTitle",
        "overview",
        "posterPath",
        "releaseDate",
        "score",
        "title",
        "voteAverage",
      ].sort(),
    );
    expect(result.score).toBe(0.5);
    expect(result.genreIds).toEqual([1, 2]);
  });

  it("rating 0 stays 0", () => {
    expect(
      normalizeMediaItem({
        id: 1,
        title: "Unrated",
        media_type: "movie",
        vote_average: 0,
      }).voteAverage,
    ).toBe(0);
    expect(
      normalizeMediaItem({
        id: 2,
        title: "Unrated Camel",
        mediaType: "movie",
        voteAverage: 0,
      }).voteAverage,
    ).toBe(0);
  });

  it("score 0 stays 0", () => {
    expect(
      normalizeMediaItem({
        id: 1,
        title: "Zero",
        media_type: "movie",
        score: 0,
      }).score,
    ).toBe(0);
  });
});
