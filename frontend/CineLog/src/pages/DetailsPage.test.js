import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";

const detailsHookReturn = vi.fn();

vi.mock("react-router", () => ({
  Link: ({ children, to, ...props }) =>
    createElement("a", { href: to, ...props }, children),
  useNavigate: () => () => {},
  useParams: () => ({ mediaType: "tv", id: "1399" }),
}));

vi.mock("../hooks/useDetails", () => ({
  default: () => detailsHookReturn(),
}));

vi.mock("../stores/authStore", () => ({
  useAuthStore: (select) => select({ user: null }),
}));

vi.mock("../hooks/useAdultPolicy", () => ({
  default: () => ({ blurAdult: false }),
}));

vi.mock("../components/VideoModal", () => ({
  default: () => null,
}));

vi.mock("../components/ListActionButton", () => ({
  default: () => null,
}));

vi.mock("../assets/missing.png", () => ({ default: "missing.png" }));

const loadedDetails = {
  id: 1399,
  title: "Game of Thrones",
  originalTitle: "Game of Thrones",
  overview: "Short overview.",
  posterPath: "/poster.jpg",
  backdropPath: "/backdrop.jpg",
  voteAverage: 8.4,
  voteCount: 1000,
  genreIds: [18],
  originalLanguage: "en",
  adult: false,
  popularity: 100,
  firstAirDate: "2011-04-17",
};

/**
 * Details page loading and loaded behavior.
 */
describe("DetailsPage", () => {
  beforeEach(() => {
    vi.resetModules();
    detailsHookReturn.mockReset();
  });

  it("shows an accessible loading state while details fetch", async () => {
    detailsHookReturn.mockReturnValue({
      details: null,
      cast: [],
      trailerUrl: null,
      loading: true,
      error: null,
      retry: () => {},
    });
    const { default: DetailsPage } = await import("./DetailsPage");
    const html = renderToStaticMarkup(createElement(DetailsPage));

    expect(html).toContain('aria-busy="true"');
    expect(html).toContain("Loading details");
    expect(html).not.toContain("Game of Thrones");
  });

  it("renders the title and overview when details are loaded", async () => {
    detailsHookReturn.mockReturnValue({
      details: loadedDetails,
      cast: [],
      trailerUrl: "https://example.com/trailer",
      loading: false,
      error: null,
      retry: () => {},
    });
    const { default: DetailsPage } = await import("./DetailsPage");
    const html = renderToStaticMarkup(createElement(DetailsPage));

    expect(html).not.toContain('aria-busy="true"');
    expect(html).toMatch(/<h1[^>]*>Game of Thrones<\/h1>/);
    expect(html).toContain("Overview");
    expect(html).toContain("Watch Trailer");
  });
});
