import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";

vi.mock("react-router", () => ({
  Link: ({ children, to, ...props }) =>
    createElement("a", { href: to, ...props }, children),
}));

vi.mock("../hooks/useAdultPolicy", () => ({
  default: () => ({ blurAdult: false }),
}));

vi.mock("../assets/missing.png", () => ({ default: "missing.png" }));

vi.mock("@/components/ui/tooltip", () => ({
  Tooltip: ({ children }) => children,
  TooltipTrigger: ({ children }) => children,
  TooltipContent: () => null,
}));

import Card from "./Card";

const sample = {
  id: 332,
  title: "Resident Evil",
  originalTitle: "Resident Evil",
  originalLanguage: "en",
  releaseDate: "2002-03-15",
  voteAverage: 6.6,
  posterPath: "/poster.jpg",
  adult: false,
};

describe("Card", () => {
  it("renders title, date and an accessible rating", () => {
    const html = renderToStaticMarkup(
      createElement(Card, {
        item: sample,
        to: "/details/movie/332",
      }),
    );
    expect(html).toContain("Resident Evil");
    expect(html).toContain("2002-03-15");
    expect(html).toContain('aria-label="rating"');
    expect(html).toContain("6.6");
    expect(html).toContain('href="/details/movie/332"');
  });
});
