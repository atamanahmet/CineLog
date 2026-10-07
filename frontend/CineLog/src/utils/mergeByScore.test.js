import { describe, expect, it } from "vitest";
import { mergeByScore } from "./media";

const M1 = { id: 1, mediaType: "MOVIE", title: "M1", score: 0.9 };
const M2 = { id: 2, mediaType: "MOVIE", title: "M2", score: 0.5 };
const M3 = { id: 3, mediaType: "MOVIE", title: "M3", score: null };
const T1 = { id: 1, mediaType: "TV", title: "T1", score: 0.8 };
const T2 = { id: 2, mediaType: "TV", title: "T2", score: 0.5 };
const T3 = { id: 3, mediaType: "TV", title: "T3", score: null };

describe("mergeByScore", () => {
  it("orders by score descending", () => {
    expect(mergeByScore([M2, M1], [T1])).toEqual([M1, T1, M2]);
  });

  it("puts null scores last", () => {
    expect(mergeByScore([M3, M1], [T3, T1])).toEqual([M1, T1, M3, T3]);
  });

  it("keeps stable order for ties", () => {
    expect(mergeByScore([M2], [T2])).toEqual([M2, T2]);
    expect(mergeByScore([M2, { ...M2, id: 9, title: "M2b" }], [])).toEqual([
      M2,
      { ...M2, id: 9, title: "M2b" },
    ]);
  });

  it("does not mutate inputs", () => {
    const movies = [M2, M1];
    const tv = [T1];
    const moviesSnap = movies.slice();
    const tvSnap = tv.slice();
    mergeByScore(movies, tv);
    expect(movies).toEqual(moviesSnap);
    expect(tv).toEqual(tvSnap);
  });

  it("handles empty inputs", () => {
    expect(mergeByScore([], [])).toEqual([]);
    expect(mergeByScore(null, undefined)).toEqual([]);
    expect(mergeByScore([M1], null)).toEqual([M1]);
    expect(mergeByScore(undefined, [T1])).toEqual([T1]);
  });
});
