import { describe, expect, it } from "vitest";
import { removeItemFromList, restoreItemToList } from "./listItems";

describe("removeItemFromList", () => {
  it("finds the right item and returns removed with index", () => {
    const items = [
      { id: 1, mediaType: "MOVIE", title: "A" },
      { id: 2, mediaType: "MOVIE", title: "B" },
      { id: 3, mediaType: "TV", title: "C" },
    ];
    const { items: next, removed } = removeItemFromList(items, 2, "MOVIE");
    expect(removed).toEqual({
      item: { id: 2, mediaType: "MOVIE", title: "B" },
      index: 1,
    });
    expect(next).toEqual([
      { id: 1, mediaType: "MOVIE", title: "A" },
      { id: 3, mediaType: "TV", title: "C" },
    ]);
  });

  it("returns null removed for no match and does not mutate input", () => {
    const items = [{ id: 1, mediaType: "MOVIE", title: "A" }];
    const freeze = [...items];
    const { items: next, removed } = removeItemFromList(items, 99, "MOVIE");
    expect(removed).toBeNull();
    expect(next).toBe(items);
    expect(items).toEqual(freeze);
  });

  it("does not confuse the same id with another media type", () => {
    const items = [
      { id: 7, mediaType: "MOVIE", title: "Film" },
      { id: 7, mediaType: "TV", title: "Show" },
    ];
    const { items: next, removed } = removeItemFromList(items, 7, "TV");
    expect(removed.index).toBe(1);
    expect(removed.item.title).toBe("Show");
    expect(next).toEqual([{ id: 7, mediaType: "MOVIE", title: "Film" }]);
  });
});

describe("restoreItemToList", () => {
  it("puts the item back at its index", () => {
    const items = [
      { id: 1, mediaType: "MOVIE" },
      { id: 3, mediaType: "MOVIE" },
    ];
    const removed = { item: { id: 2, mediaType: "MOVIE" }, index: 1 };
    expect(restoreItemToList(items, removed)).toEqual([
      { id: 1, mediaType: "MOVIE" },
      { id: 2, mediaType: "MOVIE" },
      { id: 3, mediaType: "MOVIE" },
    ]);
  });

  it("is idempotent when the item is already present", () => {
    const items = [
      { id: 1, mediaType: "MOVIE" },
      { id: 2, mediaType: "MOVIE" },
    ];
    const removed = { item: { id: 2, mediaType: "MOVIE" }, index: 0 };
    expect(restoreItemToList(items, removed)).toEqual(items);
  });

  it("clamps index past the end", () => {
    const items = [{ id: 1, mediaType: "MOVIE" }];
    const removed = { item: { id: 9, mediaType: "MOVIE" }, index: 99 };
    expect(restoreItemToList(items, removed)).toEqual([
      { id: 1, mediaType: "MOVIE" },
      { id: 9, mediaType: "MOVIE" },
    ]);
  });

  it("does nothing when removed is null", () => {
    const items = [{ id: 1, mediaType: "MOVIE" }];
    expect(restoreItemToList(items, null)).toEqual(items);
  });
});
