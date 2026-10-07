import { describe, expect, it } from "vitest";
import { buildDetailsPath } from "./media";

describe("buildDetailsPath", () => {
  it("builds a movie details path", () => {
    expect(buildDetailsPath("movie", 550)).toBe("/details/movie/550");
  });

  it("builds a tv details path", () => {
    expect(buildDetailsPath("tv", 1399)).toBe("/details/tv/1399");
  });

  it("throws on an invalid media type", () => {
    expect(() => buildDetailsPath("person", 1)).toThrow(
      'Invalid media type "person"',
    );
  });
});
