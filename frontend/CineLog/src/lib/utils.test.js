import { describe, expect, it } from "vitest";
import { cn } from "./utils";

describe("cn", () => {
  it("merges conflicting utilities and keeps responsive variants", () => {
    expect(cn("h-9", "h-11")).toBe("h-11");
    expect(cn("w-full", "sm:w-72")).toBe("w-full sm:w-72");
  });
});
