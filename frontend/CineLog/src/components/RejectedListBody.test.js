import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import RejectedListBody from "./RejectedListBody";

vi.mock("./CardPlate", () => ({
  default: () => createElement("div", { "data-card-plate": "1" }),
}));

vi.mock("./CardGridSkeleton", () => ({
  default: () => createElement("div", { "data-skeleton": "1" }),
}));

const EMPTY_TEXT =
  "Titles you mark as not interested will show here.";

describe("RejectedListBody", () => {
  it("renders the empty copy", () => {
    const html = renderToStaticMarkup(
      createElement(RejectedListBody, {
        items: [],
        hasData: true,
        isLoading: false,
        isError: false,
        refetch: () => {},
        onRestore: () => {},
      }),
    );
    expect(html).toContain(EMPTY_TEXT);
  });

  it("renders the error state with Try again", () => {
    const html = renderToStaticMarkup(
      createElement(RejectedListBody, {
        items: [],
        hasData: false,
        isLoading: false,
        isError: true,
        refetch: () => {},
        onRestore: () => {},
      }),
    );
    expect(html).toContain("Could not load this list.");
    expect(html).toContain("Try again");
  });
});
