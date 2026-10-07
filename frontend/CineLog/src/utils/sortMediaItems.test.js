import { describe, expect, it } from "vitest";
import { sortMediaItems } from "./media";

const A = { id: 1, title: "Alpha", voteAverage: 7, releaseDate: "2000-01-01" };
const B = { id: 2, title: "bravo", voteAverage: 9, releaseDate: "2010-06-15" };
const C = { id: 3, title: "Charlie", voteAverage: 9, releaseDate: "2010-06-15" };
const D = { id: 4, title: "Delta", voteAverage: null, releaseDate: null };

describe("sortMediaItems", () => {
  it("match returns original order in a new array", () => {
    const input = [A, B, C];
    const result = sortMediaItems(input, "match");
    expect(result).toEqual([A, B, C]);
    expect(result).not.toBe(input);
  });

  it("ratingDesc sorts high to low", () => {
    expect(sortMediaItems([A, B, D], "ratingDesc")).toEqual([B, A, D]);
  });

  it("newest sorts late dates first", () => {
    expect(sortMediaItems([A, B, D], "newest")).toEqual([B, A, D]);
  });

  it("oldest sorts early dates first", () => {
    expect(sortMediaItems([B, A, D], "oldest")).toEqual([A, B, D]);
  });

  it("titleAsc is case insensitive", () => {
    expect(sortMediaItems([C, B, A], "titleAsc")).toEqual([A, B, C]);
  });

  it("null voteAverage and releaseDate go last", () => {
    expect(sortMediaItems([D, A, B], "ratingDesc").at(-1)).toBe(D);
    expect(sortMediaItems([D, B, A], "newest").at(-1)).toBe(D);
    expect(sortMediaItems([D, B, A], "oldest").at(-1)).toBe(D);
  });

  it("keeps stable order for ties", () => {
    expect(sortMediaItems([B, C], "ratingDesc")).toEqual([B, C]);
    expect(sortMediaItems([C, B], "ratingDesc")).toEqual([C, B]);
    expect(sortMediaItems([B, C], "newest")).toEqual([B, C]);
    expect(sortMediaItems([C, B], "oldest")).toEqual([C, B]);
  });

  it("does not mutate the input array", () => {
    const input = [B, A, D];
    const snapshot = input.slice();
    sortMediaItems(input, "ratingDesc");
    sortMediaItems(input, "titleAsc");
    expect(input).toEqual(snapshot);
  });
});
