import { describe, expect, it } from "vitest";
import { shouldFetchNextPage } from "./useInfiniteScroll";

/**
 * Baseline args that allow a fetch.
 */
function ready(overrides = {}) {
  return {
    isIntersecting: true,
    hasNextPage: true,
    isFetchingNextPage: false,
    pagesLoaded: 1,
    maxPages: 5,
    ...overrides,
  };
}

describe("shouldFetchNextPage", () => {
  it("fetches when intersecting, has next page, idle, and under the page cap", () => {
    expect(shouldFetchNextPage(ready())).toBe(true);
  });

  it("blocks when not intersecting", () => {
    expect(shouldFetchNextPage(ready({ isIntersecting: false }))).toBe(false);
  });

  it("blocks when hasNextPage is false", () => {
    expect(shouldFetchNextPage(ready({ hasNextPage: false }))).toBe(false);
  });

  it("blocks when a next page fetch is already running", () => {
    expect(shouldFetchNextPage(ready({ isFetchingNextPage: true }))).toBe(false);
  });

  it("blocks when pagesLoaded is at the max", () => {
    expect(shouldFetchNextPage(ready({ pagesLoaded: 5, maxPages: 5 }))).toBe(
      false,
    );
  });
});
