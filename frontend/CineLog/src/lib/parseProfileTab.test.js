import { describe, expect, it } from "vitest";
import { parseProfileTab } from "./parseProfileTab";

const VALID_KEYS = [
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
  "Recommendation",
  "Find similar",
];

const FALLBACK = "Watchlist";

describe("parseProfileTab", () => {
  it("keeps a valid key", () => {
    expect(parseProfileTab("Recommendation", VALID_KEYS, FALLBACK)).toBe(
      "Recommendation",
    );
  });

  it("returns fallback for an unknown key", () => {
    expect(parseProfileTab("nonsense", VALID_KEYS, FALLBACK)).toBe(FALLBACK);
  });

  it("returns fallback for null", () => {
    expect(parseProfileTab(null, VALID_KEYS, FALLBACK)).toBe(FALLBACK);
  });
});
