import { describe, expect, it } from "vitest";
import { buildTvSeriesFacts, formatNextEpisodeLine } from "./tvSeriesFacts";

describe("buildTvSeriesFacts", () => {
  it("omits null facts", () => {
    const facts = buildTvSeriesFacts({
      status: "Ended",
      numberOfSeasons: 1,
      numberOfEpisodes: null,
      episodeRunTime: null,
      firstAirDate: "2011-04-17",
      lastAirDate: null,
      networks: [],
      createdBy: [],
    });
    const labels = facts.map((f) => f.label);
    expect(labels).toContain("Status");
    expect(labels).toContain("Seasons");
    expect(labels).toContain("First aired");
    expect(labels).not.toContain("Episodes");
    expect(labels).not.toContain("Runtime");
    expect(labels).not.toContain("Last aired");
    expect(labels).not.toContain("Networks");
    expect(labels).not.toContain("Created by");
  });

  it("includes runtime and creators when present", () => {
    const facts = buildTvSeriesFacts({
      status: "Returning Series",
      numberOfSeasons: 2,
      numberOfEpisodes: 12,
      episodeRunTime: 45,
      firstAirDate: "2020-01-01",
      lastAirDate: "2021-06-01",
      networks: ["HBO"],
      createdBy: [{ id: 1, name: "Jane Doe" }],
    });
    expect(facts.find((f) => f.label === "Runtime")?.value).toBe("45 min");
    expect(facts.find((f) => f.label === "Created by")?.creators).toEqual([
      { id: 1, name: "Jane Doe" },
    ]);
  });
});

describe("formatNextEpisodeLine", () => {
  it("formats season episode and date", () => {
    expect(
      formatNextEpisodeLine({
        seasonNumber: 2,
        episodeNumber: 5,
        airDate: "2026-03-15",
      }),
    ).toMatch(/^Next: S2E5 on /);
  });

  it("returns null when next episode missing", () => {
    expect(formatNextEpisodeLine(null)).toBeNull();
  });
});
