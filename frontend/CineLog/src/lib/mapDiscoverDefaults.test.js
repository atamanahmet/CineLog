import { describe, expect, it } from "vitest";
import { mapDiscoverDefaults } from "./mapDiscoverDefaults";

describe("mapDiscoverDefaults", () => {
  it("maps nested API fields to bounds", () => {
    const bounds = mapDiscoverDefaults({
      minVotes: { default: 100, max: 10000 },
      runtime: { min: 0, max: 400 },
      year: { min: 1800, max: 2100 },
      rating: { min: 0, max: 10 },
      maxPage: 500,
    });
    expect(bounds).toEqual({
      minVotesDefault: 100,
      voteCount: { min: 0, max: 10000 },
      runtime: { min: 0, max: 400 },
      yearRange: { min: 1800, max: 2100 },
      rating: { min: 0, max: 10 },
      maxPage: 500,
    });
  });
});
