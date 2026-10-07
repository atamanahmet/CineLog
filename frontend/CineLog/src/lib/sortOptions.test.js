import { describe, expect, it } from "vitest";
import {
  DEFAULT_DISCOVER_SORT,
  DISCOVER_SORT_MOVIE_TO_TV,
  DISCOVER_SORT_TV_TO_MOVIE,
  mapDiscoverSortForMediaType,
} from "./sortOptions";

describe("mapDiscoverSortForMediaType", () => {
  it("maps movie date and title keys to TV equivalents", () => {
    expect(mapDiscoverSortForMediaType("primary_release_date.desc", "tv")).toBe(
      "first_air_date.desc",
    );
    expect(mapDiscoverSortForMediaType("primary_release_date.asc", "tv")).toBe(
      "first_air_date.asc",
    );
    expect(mapDiscoverSortForMediaType("title.asc", "tv")).toBe("name.asc");
    expect(mapDiscoverSortForMediaType("title.desc", "tv")).toBe("name.desc");
  });

  it("maps TV date and title keys back to movie equivalents", () => {
    expect(
      mapDiscoverSortForMediaType("first_air_date.desc", "movie"),
    ).toBe("primary_release_date.desc");
    expect(mapDiscoverSortForMediaType("first_air_date.asc", "movie")).toBe(
      "primary_release_date.asc",
    );
    expect(mapDiscoverSortForMediaType("name.asc", "movie")).toBe("title.asc");
    expect(mapDiscoverSortForMediaType("name.desc", "movie")).toBe("title.desc");
  });

  it("passes through shared keys and nullish values", () => {
    expect(mapDiscoverSortForMediaType("popularity.desc", "tv")).toBe(
      "popularity.desc",
    );
    expect(mapDiscoverSortForMediaType("vote_average.asc", "movie")).toBe(
      "vote_average.asc",
    );
    expect(mapDiscoverSortForMediaType(null, "tv")).toBeNull();
    expect(mapDiscoverSortForMediaType(undefined, "movie")).toBeUndefined();
  });

  it("resets keys with no equivalent to the default discover sort", () => {
    expect(mapDiscoverSortForMediaType("release_date.desc", "tv")).toBe(
      DEFAULT_DISCOVER_SORT,
    );
    expect(mapDiscoverSortForMediaType("unknown.sort", "movie")).toBe(
      DEFAULT_DISCOVER_SORT,
    );
  });

  it("keeps movie→TV mapping table complete for date and title", () => {
    expect(DISCOVER_SORT_MOVIE_TO_TV["primary_release_date.desc"]).toBe(
      "first_air_date.desc",
    );
    expect(DISCOVER_SORT_MOVIE_TO_TV["title.asc"]).toBe("name.asc");
    expect(DISCOVER_SORT_TV_TO_MOVIE["first_air_date.desc"]).toBe(
      "primary_release_date.desc",
    );
    expect(DISCOVER_SORT_TV_TO_MOVIE["name.desc"]).toBe("title.desc");
  });
});
