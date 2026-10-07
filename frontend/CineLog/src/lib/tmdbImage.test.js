import { describe, expect, it } from "vitest";
import { backdropSrcSet } from "./tmdbImage";

describe("backdropSrcSet", () => {
  it("lists w780, w1280, then original at 1920w", () => {
    expect(backdropSrcSet("/hero.jpg")).toBe(
      "https://image.tmdb.org/t/p/w780/hero.jpg 780w, https://image.tmdb.org/t/p/w1280/hero.jpg 1280w, https://image.tmdb.org/t/p/original/hero.jpg 1920w",
    );
  });

  it("returns nothing for an empty path", () => {
    expect(backdropSrcSet("")).toBe("");
    expect(backdropSrcSet(null)).toBe("");
  });
});
