import { QueryClient } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { REJECTED_LIST_TYPE } from "./useRejectTitle";
import {
  performRestoreRejected,
  REJECTED_QUERY_KEY,
} from "./useRestoreRejected";

describe("performRestoreRejected", () => {
  let queryClient;
  let deleteRejected;
  let toastError;

  beforeEach(() => {
    queryClient = new QueryClient();
    queryClient.setQueryData(REJECTED_QUERY_KEY, [
      { id: 10, mediaType: "MOVIE", title: "Gone" },
      { id: 11, mediaType: "MOVIE", title: "Stay" },
    ]);
    deleteRejected = vi.fn().mockResolvedValue(undefined);
    toastError = vi.fn();
  });

  it("success calls API once with rejectedlist and removes the item", async () => {
    await performRestoreRejected(
      { id: 10, mediaType: "MOVIE", title: "Gone" },
      { queryClient, deleteRejected, toastError },
    );
    expect(deleteRejected).toHaveBeenCalledTimes(1);
    expect(deleteRejected).toHaveBeenCalledWith(
      "movie",
      REJECTED_LIST_TYPE,
      10,
    );
    expect(queryClient.getQueryData(REJECTED_QUERY_KEY).map((i) => i.id)).toEqual([
      11,
    ]);
    expect(toastError).not.toHaveBeenCalled();
  });

  it("failure restores the item and calls toast.error", async () => {
    deleteRejected.mockRejectedValue(new Error("network"));
    const before = [...queryClient.getQueryData(REJECTED_QUERY_KEY)];
    await performRestoreRejected(
      { id: 10, mediaType: "MOVIE", title: "Gone" },
      { queryClient, deleteRejected, toastError },
    );
    expect(queryClient.getQueryData(REJECTED_QUERY_KEY)).toEqual(before);
    expect(toastError).toHaveBeenCalledWith(
      "Could not restore this title. Please try again.",
    );
  });
});
