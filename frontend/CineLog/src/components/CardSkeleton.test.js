import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { CARD_SIZE_CLASS, CardSkeleton } from "../components/Card";
import CardGridSkeleton from "../components/CardGridSkeleton";
import { TMDB_PAGE_SIZE } from "../hooks/useCatalogInfiniteQuery";

describe("CardSkeleton", () => {
  it("shares the root size classes with Card and is aria-hidden", () => {
    const html = renderToStaticMarkup(createElement(CardSkeleton));
    for (const cls of CARD_SIZE_CLASS.split(/\s+/)) {
      expect(html).toContain(cls);
    }
    expect(html).toContain('aria-hidden="true"');
  });
});

describe("CardGridSkeleton", () => {
  it("renders the expected count", () => {
    const html = renderToStaticMarkup(
      createElement(CardGridSkeleton, { count: TMDB_PAGE_SIZE }),
    );
    const cards = html.match(/sm:w-\[215px\]/g) ?? [];
    expect(cards).toHaveLength(TMDB_PAGE_SIZE);
    expect(html).toContain('aria-busy="true"');
  });
});
