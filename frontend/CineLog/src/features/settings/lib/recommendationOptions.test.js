import { describe, expect, it } from "vitest";
import {
  REC_MAX_RESULTS_PRESETS,
  buildMaxResultsOptions,
} from "./recommendationOptions";
import {
  RECOMMENDATION_SKELETON_MAX,
  recommendationSkeletonCount,
} from "../../../lib/recommendationSkeleton";

describe("buildMaxResultsOptions", () => {
  it("builds options for limit 50", () => {
    expect(buildMaxResultsOptions(50)).toEqual([10, 25, 50]);
  });

  it("builds options for limit 75", () => {
    expect(buildMaxResultsOptions(75)).toEqual([10, 25, 50, 75]);
  });

  it("builds options for limit 100", () => {
    expect(buildMaxResultsOptions(100)).toEqual([10, 25, 50, 100]);
    expect(REC_MAX_RESULTS_PRESETS).toEqual([10, 25, 50, 100]);
  });
});

describe("recommendationSkeletonCount", () => {
  it("returns min of maxResults and 24", () => {
    expect(RECOMMENDATION_SKELETON_MAX).toBe(24);
    expect(recommendationSkeletonCount(10)).toBe(10);
    expect(recommendationSkeletonCount(24)).toBe(24);
    expect(recommendationSkeletonCount(50)).toBe(24);
    expect(recommendationSkeletonCount(100)).toBe(24);
  });
});
