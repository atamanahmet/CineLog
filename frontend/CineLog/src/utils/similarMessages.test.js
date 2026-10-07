import { describe, expect, it } from "vitest";
import { similarEmptyMessage } from "./similarMessages";

describe("similarEmptyMessage", () => {
  it("one seed text", () => {
    expect(similarEmptyMessage(1)).toBe(
      "We don't have enough data on this title yet. Try another one.",
    );
  });

  it("two seeds text", () => {
    expect(similarEmptyMessage(2)).toBe(
      "We couldn't find close matches for these titles yet.",
    );
  });
});
