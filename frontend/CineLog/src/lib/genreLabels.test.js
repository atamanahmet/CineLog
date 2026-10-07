import { describe, expect, it } from "vitest";
import { genreLabels } from "./genreLabels";

describe("genreLabels", () => {
  it("maps movie ids", () => {
    expect(genreLabels([18, 28, 35])).toEqual(["Drama", "Action", "Comedy"]);
  });

  it("maps tv ids", () => {
    expect(genreLabels([10759, 10765, 10768])).toEqual([
      "Action & Adventure",
      "Sci-Fi & Fantasy",
      "War & Politics",
    ]);
  });

  it("skips an unknown id", () => {
    expect(genreLabels([18, 999999, 10765])).toEqual([
      "Drama",
      "Sci-Fi & Fantasy",
    ]);
  });
});
