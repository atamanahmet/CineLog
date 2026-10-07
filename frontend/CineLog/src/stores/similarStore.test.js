import { beforeEach, describe, expect, it } from "vitest";
import { MAX_SEEDS, useSimilarStore } from "./similarStore";

const sample = (id, type = "MOVIE") => ({
  tmdbId: id,
  mediaType: type,
  title: `Title ${id}`,
  posterPath: `/p${id}.jpg`,
  year: 2000 + id,
});

describe("similarStore", () => {
  beforeEach(() => {
    useSimilarStore.getState().reset();
  });

  it("adds a seed", () => {
    useSimilarStore.getState().addSeed(sample(1));
    expect(useSimilarStore.getState().seeds).toEqual([sample(1)]);
  });

  it("ignores duplicate tmdbId and mediaType", () => {
    useSimilarStore.getState().addSeed(sample(1));
    useSimilarStore.getState().addSeed(sample(1));
    expect(useSimilarStore.getState().seeds).toHaveLength(1);
  });

  it("allows same tmdbId with different mediaType", () => {
    useSimilarStore.getState().addSeed(sample(1, "MOVIE"));
    useSimilarStore.getState().addSeed(sample(1, "TV"));
    expect(useSimilarStore.getState().seeds).toHaveLength(2);
  });

  it("ignores the 11th seed", () => {
    for (let i = 1; i <= MAX_SEEDS + 1; i += 1) {
      useSimilarStore.getState().addSeed(sample(i));
    }
    expect(useSimilarStore.getState().seeds).toHaveLength(MAX_SEEDS);
  });

  it("removes one seed", () => {
    useSimilarStore.getState().addSeed(sample(1));
    useSimilarStore.getState().addSeed(sample(2));
    useSimilarStore.getState().removeSeed(1, "MOVIE");
    expect(useSimilarStore.getState().seeds).toEqual([sample(2)]);
  });

  it("clearSeeds drops all seeds", () => {
    useSimilarStore.getState().addSeed(sample(1));
    useSimilarStore.getState().addSeed(sample(2));
    useSimilarStore.getState().clearSeeds();
    expect(useSimilarStore.getState().seeds).toEqual([]);
  });

  it("reset restores view and empty seeds", () => {
    useSimilarStore.getState().addSeed(sample(1));
    useSimilarStore.getState().setView("all");
    useSimilarStore.getState().reset();
    expect(useSimilarStore.getState().seeds).toEqual([]);
    expect(useSimilarStore.getState().view).toBe("movie");
  });
});
