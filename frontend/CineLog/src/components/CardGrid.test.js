import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import CardGrid, { CARD_GRID_BASE_CLASS } from "./CardGrid";

describe("CardGrid", () => {
  it("renders children with phone auto-fill and sm flex classes", () => {
    const html = renderToStaticMarkup(
      createElement(CardGrid, null, createElement("span", null, "child")),
    );
    expect(html).toContain("child");
    for (const cls of CARD_GRID_BASE_CLASS.split(/\s+/)) {
      expect(html).toContain(cls);
    }
    expect(CARD_GRID_BASE_CLASS).toContain(
      "grid-cols-[repeat(auto-fill,minmax(130px,1fr))]",
    );
    expect(CARD_GRID_BASE_CLASS).toContain("gap-3");
    expect(CARD_GRID_BASE_CLASS).toContain("sm:flex");
    expect(CARD_GRID_BASE_CLASS).toContain("sm:flex-row");
    expect(CARD_GRID_BASE_CLASS).toContain("sm:flex-wrap");
    expect(CARD_GRID_BASE_CLASS).toContain("sm:justify-center");
    expect(CARD_GRID_BASE_CLASS).toContain("sm:gap-5");
  });
});
