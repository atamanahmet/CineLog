import { QueryClient } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  performRejectTitle,
  REJECTED_LIST_TYPE,
} from "./useRejectTitle";

describe("performRejectTitle", () => {
  let lists;
  let queryClient;
  let putRejected;
  let toastError;

  beforeEach(() => {
    lists = {
      watchlist: [
        { id: 10, mediaType: "MOVIE", title: "In list" },
        { id: 99, mediaType: "MOVIE", title: "Keep" },
      ],
      lovedlist: [{ id: 10, mediaType: "MOVIE", title: "Loved" }],
      recommendations: {
        movie: [
          { id: 10, mediaType: "MOVIE", title: "Rec" },
          { id: 11, mediaType: "MOVIE", title: "Other" },
        ],
        tv: null,
      },
    };
    queryClient = new QueryClient();
    queryClient.setQueryData(["similar", "MOVIE", "k1"], [
      { id: 10, mediaType: "MOVIE", title: "Sim" },
      { id: 12, mediaType: "MOVIE", title: "Stay" },
    ]);
    queryClient.setQueryData(
      ["recommendationFiltered", "MOVIE", "878", "16"],
      [
        { id: 10, mediaType: "MOVIE", title: "Filtered" },
        { id: 13, mediaType: "MOVIE", title: "Keep filtered" },
      ],
    );
    putRejected = vi.fn().mockResolvedValue(undefined);
    toastError = vi.fn();
  });

  it("success calls API once with rejectedlist and removes from store and similar cache", async () => {
    const item = { id: 10, mediaType: "MOVIE", title: "Rec" };
    await performRejectTitle(item, {
      getListsState: () => lists,
      setListsState: (partial) => {
        lists = { ...lists, ...partial };
      },
      queryClient,
      putRejected,
      toastError,
    });

    expect(putRejected).toHaveBeenCalledTimes(1);
    expect(putRejected).toHaveBeenCalledWith("movie", 10, REJECTED_LIST_TYPE);
    expect(lists.recommendations.movie.map((i) => i.id)).toEqual([11]);
    expect(lists.watchlist.map((i) => i.id)).toEqual([99]);
    expect(lists.lovedlist).toEqual([]);
    expect(
      queryClient.getQueryData(["similar", "MOVIE", "k1"]).map((i) => i.id),
    ).toEqual([12]);
    expect(
      queryClient
        .getQueryData(["recommendationFiltered", "MOVIE", "878", "16"])
        .map((i) => i.id),
    ).toEqual([13]);
    expect(toastError).not.toHaveBeenCalled();
  });

  it("failure restores everything and calls toast.error", async () => {
    putRejected.mockRejectedValue(new Error("network"));
    const item = { id: 10, mediaType: "MOVIE", title: "Rec" };
    const beforeRec = [...lists.recommendations.movie];
    const beforeWatch = [...lists.watchlist];
    const beforeLoved = [...lists.lovedlist];
    const beforeSim = [...queryClient.getQueryData(["similar", "MOVIE", "k1"])];
    const beforeFiltered = [
      ...queryClient.getQueryData([
        "recommendationFiltered",
        "MOVIE",
        "878",
        "16",
      ]),
    ];

    await performRejectTitle(item, {
      getListsState: () => lists,
      setListsState: (partial) => {
        lists = { ...lists, ...partial };
      },
      queryClient,
      putRejected,
      toastError,
    });

    expect(lists.recommendations.movie).toEqual(beforeRec);
    expect(lists.watchlist).toEqual(beforeWatch);
    expect(lists.lovedlist).toEqual(beforeLoved);
    expect(queryClient.getQueryData(["similar", "MOVIE", "k1"])).toEqual(
      beforeSim,
    );
    expect(
      queryClient.getQueryData([
        "recommendationFiltered",
        "MOVIE",
        "878",
        "16",
      ]),
    ).toEqual(beforeFiltered);
    expect(toastError).toHaveBeenCalledWith(
      "Could not hide this title. Please try again.",
    );
  });
});
