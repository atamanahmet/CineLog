import { describe, expect, it } from "vitest";
import { omitListedItems } from "./listItems";

describe("omitListedItems", () => {
  it("removes titles present in other lists by id and mediaType", () => {
    const items = [
      { id: 1, mediaType: "MOVIE" },
      { id: 2, mediaType: "MOVIE" },
      { id: 3, mediaType: "TV" },
    ];
    const watchlist = [{ id: 2, mediaType: "MOVIE" }];
    const lovedlist = [{ id: 3, mediaType: "TV" }];
    expect(omitListedItems(items, watchlist, lovedlist)).toEqual([
      { id: 1, mediaType: "MOVIE" },
    ]);
  });

  it("does not confuse the same id with the other media type", () => {
    const items = [
      { id: 7, mediaType: "MOVIE" },
      { id: 7, mediaType: "TV" },
    ];
    const watchlist = [{ id: 7, mediaType: "MOVIE" }];
    expect(omitListedItems(items, watchlist)).toEqual([
      { id: 7, mediaType: "TV" },
    ]);
  });

  it("does not mutate its input", () => {
    const items = [
      { id: 1, mediaType: "MOVIE" },
      { id: 2, mediaType: "MOVIE" },
    ];
    const freeze = items.map((item) => ({ ...item }));
    omitListedItems(items, [{ id: 1, mediaType: "MOVIE" }]);
    expect(items).toEqual(freeze);
  });

  it("handles empty and null lists", () => {
    const items = [{ id: 1, mediaType: "MOVIE" }];
    expect(omitListedItems(items, null, undefined, [])).toEqual(items);
    expect(omitListedItems(null, items)).toEqual([]);
    expect(omitListedItems([], items)).toEqual([]);
  });
});
