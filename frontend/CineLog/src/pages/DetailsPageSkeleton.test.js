import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import DetailsPageSkeleton from "./DetailsPageSkeleton";

describe("DetailsPageSkeleton", () => {
  it("exposes a busy loading region without decorative images", () => {
    const html = renderToStaticMarkup(createElement(DetailsPageSkeleton));

    expect(html).toContain('aria-busy="true"');
    expect(html).toContain("Loading details");
    expect(html).not.toContain("<img");
  });
});
