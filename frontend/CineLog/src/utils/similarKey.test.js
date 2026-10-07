import { describe, expect, it } from "vitest";
import { similarKey } from "./similarKey";

describe("similarKey", () => {
  it("same key for different seed order", () => {
    const a = [
      { tmdbId: 2, mediaType: "TV" },
      { tmdbId: 1, mediaType: "MOVIE" },
    ];
    const b = [
      { tmdbId: 1, mediaType: "MOVIE" },
      { tmdbId: 2, mediaType: "TV" },
    ];
    expect(similarKey(a)).toBe(similarKey(b));
  });

  it("different key for different seeds", () => {
    const a = [{ tmdbId: 1, mediaType: "MOVIE" }];
    const b = [{ tmdbId: 2, mediaType: "MOVIE" }];
    expect(similarKey(a)).not.toBe(similarKey(b));
  });
});
