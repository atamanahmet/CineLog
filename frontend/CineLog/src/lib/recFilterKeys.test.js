import { describe, expect, it } from "vitest";
import { recFilterActiveKeys } from "./recFilterKeys";

describe("recFilterActiveKeys", () => {
  it("returns year and rating for every view (genres are server-side)", () => {
    expect(recFilterActiveKeys("movie")).toEqual(["yearRange", "rating"]);
    expect(recFilterActiveKeys("tv")).toEqual(["yearRange", "rating"]);
    expect(recFilterActiveKeys("all")).toEqual(["yearRange", "rating"]);
  });
});
