import { describe, expect, it, vi } from "vitest";
import { fetchDiscoverDefaults } from "./useDiscoverDefaults";

vi.mock("../api/axiosInstance", () => ({
  default: {
    get: vi.fn(),
  },
}));

import api from "../api/axiosInstance";

describe("fetchDiscoverDefaults", () => {
  it("maps API payload into bounds", async () => {
    api.get.mockResolvedValue({
      data: {
        minVotes: { default: 100, max: 10000 },
        runtime: { min: 0, max: 400 },
        year: { min: 1800, max: 2100 },
        rating: { min: 0, max: 10 },
        maxPage: 500,
      },
    });
    const bounds = await fetchDiscoverDefaults();
    expect(bounds.minVotesDefault).toBe(100);
    expect(bounds.runtime.max).toBe(400);
    expect(bounds.yearRange.min).toBe(1800);
  });

  it("rejects when the request fails", async () => {
    api.get.mockRejectedValue(new Error("network"));
    await expect(fetchDiscoverDefaults()).rejects.toThrow("network");
  });
});
