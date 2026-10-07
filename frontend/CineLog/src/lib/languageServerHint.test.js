import { describe, expect, it } from "vitest";
import { languageServerHint } from "./languageServerHint";

describe("languageServerHint", () => {
  it("returns null when zero languages are included", () => {
    expect(languageServerHint({ include: [], exclude: ["en"] })).toBeNull();
    expect(languageServerHint(null)).toBeNull();
  });

  it("returns the single included language", () => {
    expect(languageServerHint({ include: ["en"], exclude: [] })).toBe("en");
    expect(
      languageServerHint({ include: ["ja"], exclude: ["en"] }),
    ).toBe("ja");
  });

  it("returns null when two or more languages are included", () => {
    expect(
      languageServerHint({ include: ["en", "tr"], exclude: [] }),
    ).toBeNull();
  });
});
