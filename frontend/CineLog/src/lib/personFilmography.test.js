import { describe, expect, it } from "vitest";
import {
  collectDepartments,
  filterByDepartment,
  filterByMediaType,
  filterAppearances,
  groupCreditsByYear,
  isAppearanceCredit,
  mapPersonDetail,
  resolveDefaultDepartment,
} from "./personFilmography";

function credit(partial) {
  return {
    mediaType: "movie",
    tmdbId: 1,
    title: "Title",
    posterPath: "/p.jpg",
    date: "2020-01-01",
    voteAverage: 7,
    genreIds: [],
    roles: [{ kind: "cast", label: "Lead", department: null, episodeCount: null }],
    ...partial,
  };
}

describe("mapPersonDetail", () => {
  it("maps nested person and credits from A1 payload", () => {
    const mapped = mapPersonDetail({
      person: {
        id: 287,
        name: "Brad Pitt",
        biography: "Bio",
        birthday: "1963-12-18",
        deathday: null,
        placeOfBirth: "USA",
        profilePath: "/x.jpg",
        knownForDepartment: "Acting",
        alsoKnownAs: ["A", "B"],
      },
      credits: [
        {
          mediaType: "movie",
          tmdbId: 10,
          title: "Fight Club",
          posterPath: "/f.jpg",
          date: "1999-10-15",
          voteAverage: 8.4,
          genreIds: [18],
          roles: [
            { kind: "cast", label: "Tyler", department: null, episodeCount: null },
          ],
        },
      ],
    });
    expect(mapped.person.name).toBe("Brad Pitt");
    expect(mapped.credits).toHaveLength(1);
    expect(mapped.credits[0].tmdbId).toBe(10);
  });

  it("returns empty person and credits for null payload", () => {
    expect(mapPersonDetail(null)).toEqual({ person: null, credits: [] });
  });
});

describe("filterByMediaType", () => {
  const credits = [
    credit({ tmdbId: 1, mediaType: "movie", title: "M" }),
    credit({ tmdbId: 2, mediaType: "tv", title: "T" }),
  ];

  it("keeps all when tab is all", () => {
    expect(filterByMediaType(credits, "all")).toHaveLength(2);
  });

  it("keeps only movies when tab is movie", () => {
    expect(filterByMediaType(credits, "movie").map((c) => c.tmdbId)).toEqual([
      1,
    ]);
  });

  it("keeps only tv when tab is tv", () => {
    expect(filterByMediaType(credits, "tv").map((c) => c.tmdbId)).toEqual([2]);
  });
});

describe("department filtering", () => {
  const credits = [
    credit({
      tmdbId: 1,
      title: "Acted",
      roles: [{ kind: "cast", label: "Hero", department: null, episodeCount: null }],
    }),
    credit({
      tmdbId: 2,
      title: "Directed",
      roles: [
        {
          kind: "crew",
          label: "Director",
          department: "Directing",
          episodeCount: null,
        },
      ],
    }),
    credit({
      tmdbId: 3,
      title: "Both",
      roles: [
        { kind: "cast", label: "Self", department: null, episodeCount: null },
        {
          kind: "crew",
          label: "Writer",
          department: "Writing",
          episodeCount: null,
        },
      ],
    }),
  ];

  it("collectDepartments lists only departments present in data", () => {
    expect(collectDepartments(credits)).toEqual([
      "Acting",
      "Directing",
      "Writing",
    ]);
  });

  it("filterByDepartment keeps credits with a matching role department", () => {
    expect(
      filterByDepartment(credits, "Directing").map((c) => c.tmdbId),
    ).toEqual([2]);
    expect(filterByDepartment(credits, "Acting").map((c) => c.tmdbId)).toEqual([
      1, 3,
    ]);
  });

  it("resolveDefaultDepartment prefers known-for when present", () => {
    expect(resolveDefaultDepartment(["Directing", "Acting"], "Acting")).toBe(
      "Acting",
    );
    expect(resolveDefaultDepartment(["Directing"], "Acting")).toBe("Directing");
  });
});

describe("appearances rule: hide talk/news/reality when only self-like roles", () => {
  it("hides talk-show-only credit with Self role when showAppearances is off", () => {
    const talk = credit({
      tmdbId: 50,
      mediaType: "tv",
      title: "Tonight Show",
      genreIds: [10767],
      roles: [
        { kind: "cast", label: "Self", department: null, episodeCount: 2 },
      ],
    });
    expect(isAppearanceCredit(talk)).toBe(true);
    expect(filterAppearances([talk], false)).toHaveLength(0);
    expect(filterAppearances([talk], true)).toHaveLength(1);
  });

  it("keeps talk-show credit with a real acting role (not only self-like)", () => {
    const talkWithRole = credit({
      tmdbId: 51,
      mediaType: "tv",
      title: "Sketch Night",
      genreIds: [10767],
      roles: [
        {
          kind: "cast",
          label: "Detective Miles",
          department: null,
          episodeCount: 1,
        },
      ],
    });
    expect(isAppearanceCredit(talkWithRole)).toBe(false);
    expect(filterAppearances([talkWithRole], false)).toHaveLength(1);
  });

  it("keeps scripted-show credit even when role is Self (no appearance genres)", () => {
    const scripted = credit({
      tmdbId: 52,
      mediaType: "movie",
      title: "Documentary",
      genreIds: [99],
      roles: [
        { kind: "cast", label: "Himself", department: null, episodeCount: null },
      ],
    });
    expect(isAppearanceCredit(scripted)).toBe(false);
    expect(filterAppearances([scripted], false)).toHaveLength(1);
  });

  it("hides news and reality with only self-like roles", () => {
    const news = credit({
      tmdbId: 53,
      mediaType: "tv",
      genreIds: [10763],
      roles: [
        { kind: "cast", label: "Herself", department: null, episodeCount: null },
      ],
    });
    const reality = credit({
      tmdbId: 54,
      mediaType: "tv",
      genreIds: [10764],
      roles: [
        {
          kind: "cast",
          label: "Self - Host",
          department: null,
          episodeCount: null,
        },
      ],
    });
    expect(isAppearanceCredit(news)).toBe(true);
    expect(isAppearanceCredit(reality)).toBe(true);
  });

  it("keeps appearance-genre credit when a crew role is present", () => {
    const mixed = credit({
      tmdbId: 55,
      genreIds: [10767],
      roles: [
        { kind: "cast", label: "Self", department: null, episodeCount: null },
        {
          kind: "crew",
          label: "Director",
          department: "Directing",
          episodeCount: null,
        },
      ],
    });
    expect(isAppearanceCredit(mixed)).toBe(false);
  });
});

describe("groupCreditsByYear", () => {
  it("puts undated items first under Upcoming / TBA, then newest year first", () => {
    const credits = [
      credit({ tmdbId: 1, title: "Old", date: "2010-05-01" }),
      credit({ tmdbId: 2, title: "TBA", date: null }),
      credit({ tmdbId: 3, title: "New", date: "2024-01-01" }),
      credit({ tmdbId: 4, title: "Blank", date: "" }),
    ];
    const groups = groupCreditsByYear(credits);
    expect(groups.map((g) => g.label)).toEqual([
      "Upcoming / TBA",
      "2024",
      "2010",
    ]);
    expect(groups[0].credits.map((c) => c.tmdbId)).toEqual([2, 4]);
    expect(groups[1].credits.map((c) => c.tmdbId)).toEqual([3]);
    expect(groups[2].credits.map((c) => c.tmdbId)).toEqual([1]);
  });

  it("preserves encounter order within a year", () => {
    const credits = [
      credit({ tmdbId: 1, date: "2020-01-01", title: "A" }),
      credit({ tmdbId: 2, date: "2020-06-01", title: "B" }),
    ];
    expect(groupCreditsByYear(credits)[0].credits.map((c) => c.tmdbId)).toEqual(
      [1, 2],
    );
  });
});
