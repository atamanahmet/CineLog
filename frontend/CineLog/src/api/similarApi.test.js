import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("./axiosInstance", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import api from "./axiosInstance";
import { searchTitles } from "./similarApi";

describe("searchTitles", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.get.mockResolvedValue({
      data: { page: 1, results: [], totalPages: 1, totalResults: 0 },
    });
  });

  it("omits minVotes from params when it is not given", async () => {
    await searchTitles("MOVIE", "matrix", 1);
    expect(api.get).toHaveBeenCalledWith("/movie/search", {
      params: { query: "matrix", page: 1 },
    });
    const params = api.get.mock.calls[0][1].params;
    expect(params).not.toHaveProperty("minVotes");
  });

  it("sends minVotes when the modal passes it", async () => {
    await searchTitles("TV", "office", 2, 50);
    expect(api.get).toHaveBeenCalledWith("/tv/search", {
      params: { query: "office", page: 2, minVotes: 50 },
    });
  });
});
