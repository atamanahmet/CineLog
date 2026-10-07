import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("react-router", () => ({
  Link: ({ children, to, ...props }) =>
    createElement("a", { href: to, ...props }, children),
}));

vi.mock("../stores/authStore", () => ({
  useAuthStore: (sel) => sel({ user: { id: 1 } }),
}));

vi.mock("./ListActionButton", () => ({
  default: () => createElement("span", { "data-list-btn": "1" }),
}));

vi.mock("./RejectActionButton", () => ({
  default: () =>
    createElement("button", { "aria-label": "Not interested" }, "X"),
}));

vi.mock("./Card", () => ({
  default: ({ to }) => createElement("div", { "data-card": "1", "data-to": to }),
}));

import CardPlate from "./CardPlate";

const sample = [{ id: 1, mediaType: "MOVIE", title: "A" }];

describe("CardPlate", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("without onReject renders no reject button", () => {
    const html = renderToStaticMarkup(
      createElement(CardPlate, { data: sample, mediaType: "movie" }),
    );
    expect(html).not.toContain('aria-label="Not interested"');
  });

  it("with onReject renders a reject button", () => {
    const html = renderToStaticMarkup(
      createElement(CardPlate, {
        data: sample,
        mediaType: "movie",
        onReject: () => {},
      }),
    );
    expect(html).toContain('aria-label="Not interested"');
  });
});
