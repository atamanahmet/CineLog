import { describe, expect, it } from "vitest";
import { shouldFetchMore } from "./shouldFetchMore";

function ready(overrides = {}) {
  return {
    visibleCount: 5,
    minVisible: 20,
    hasNextPage: true,
    isFetching: false,
    extraPagesFetched: 0,
    maxExtraPages: 5,
    enabled: true,
    ...overrides,
  };
}

describe("shouldFetchMore", () => {
  it("fetches when below minVisible with budget left", () => {
    expect(shouldFetchMore(ready())).toBe(true);
  });

  it("blocks when visibleCount meets minVisible", () => {
    expect(shouldFetchMore(ready({ visibleCount: 20 }))).toBe(false);
  });

  it("blocks when hasNextPage is false", () => {
    expect(shouldFetchMore(ready({ hasNextPage: false }))).toBe(false);
  });

  it("blocks while fetching", () => {
    expect(shouldFetchMore(ready({ isFetching: true }))).toBe(false);
  });

  it("blocks when disabled", () => {
    expect(shouldFetchMore(ready({ enabled: false }))).toBe(false);
  });

  it("blocks at the hard extra-page cap", () => {
    expect(
      shouldFetchMore(ready({ extraPagesFetched: 5, maxExtraPages: 5 })),
    ).toBe(false);
    expect(
      shouldFetchMore(ready({ extraPagesFetched: 4, maxExtraPages: 5 })),
    ).toBe(true);
  });
});
