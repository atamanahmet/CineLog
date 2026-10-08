import { describe, expect, it } from "vitest";
import { hasRating } from "./hasRating";

describe("hasRating", () => {
  it("is false for 0, null and undefined", () => {
    expect(hasRating(0)).toBe(false);
    expect(hasRating(null)).toBe(false);
    expect(hasRating(undefined)).toBe(false);
  });

  it("is true for a positive rating", () => {
    expect(hasRating(7.3)).toBe(true);
  });
});
