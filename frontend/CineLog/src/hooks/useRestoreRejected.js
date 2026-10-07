import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { removeListEntry } from "../api/listsApi";
import { resolveMediaType } from "../utils/media";
import { removeItemFromList, restoreItemToList } from "../utils/listItems";
import { REJECTED_LIST_TYPE } from "./useRejectTitle";

export const REJECTED_QUERY_KEY = ["rejected"];

/**
 * Optimistic restore from the rejected list. Exported for unit tests.
 */
export async function performRestoreRejected(item, deps) {
  const { queryClient, deleteRejected, toastError } = deps;
  const tmdbId = item?.id;
  if (tmdbId == null) {
    return;
  }
  const mediaType = item?.mediaType ?? item?.media_type ?? resolveMediaType(item);
  const uiType = resolveMediaType(item);
  const current = queryClient.getQueryData(REJECTED_QUERY_KEY);
  const { items, removed } = removeItemFromList(
    Array.isArray(current) ? current : [],
    tmdbId,
    mediaType,
  );
  queryClient.setQueryData(REJECTED_QUERY_KEY, items);

  try {
    await deleteRejected(uiType, REJECTED_LIST_TYPE, tmdbId);
  } catch {
    const latest = queryClient.getQueryData(REJECTED_QUERY_KEY);
    queryClient.setQueryData(
      REJECTED_QUERY_KEY,
      restoreItemToList(Array.isArray(latest) ? latest : [], removed),
    );
    toastError("Could not restore this title. Please try again.");
  }
}

/**
 * Returns restoreTitle for Not interested cards.
 */
export default function useRestoreRejected() {
  const queryClient = useQueryClient();

  return useCallback(
    (item) =>
      performRestoreRejected(item, {
        queryClient,
        deleteRejected: removeListEntry,
        toastError: (message) => toast.error(message),
      }),
    [queryClient],
  );
}
