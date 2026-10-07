import { describe, expect, it } from "vitest";
import { flattenSeedSearchPages } from "./seedSearchResults";

describe("flattenSeedSearchPages", () => {
  it("removes duplicates across pages and keeps the first", () => {
    const pages = [
      {
        items: [
          { id: 1, mediaType: "MOVIE", posterPath: "/a.jpg", title: "First" },
          { id: 2, mediaType: "MOVIE", posterPath: "/b.jpg", title: "Second" },
        ],
      },
      {
        items: [
          { id: 1, mediaType: "MOVIE", posterPath: "/a2.jpg", title: "Dup" },
          { id: 3, mediaType: "MOVIE", posterPath: "/c.jpg", title: "Third" },
        ],
      },
    ];
    const result = flattenSeedSearchPages(pages);
    expect(result.map((item) => item.title)).toEqual([
      "First",
      "Second",
      "Third",
    ]);
  });

  it("drops titles without a poster", () => {
    const pages = [
      {
        items: [
          { id: 1, mediaType: "TV", posterPath: "/a.jpg", title: "Has" },
          { id: 2, mediaType: "TV", posterPath: null, title: "Missing" },
          { id: 3, mediaType: "TV", posterPath: "", title: "Empty" },
        ],
      },
    ];
    const result = flattenSeedSearchPages(pages);
    expect(result.map((item) => item.title)).toEqual(["Has"]);
  });

  it("keeps order and treats mediaType plus id as the duplicate key", () => {
    const pages = [
      {
        items: [
          { id: 1, mediaType: "MOVIE", posterPath: "/m.jpg", title: "Movie" },
          { id: 1, mediaType: "TV", posterPath: "/t.jpg", title: "Show" },
        ],
      },
    ];
    const result = flattenSeedSearchPages(pages);
    expect(result.map((item) => item.title)).toEqual(["Movie", "Show"]);
  });
});
