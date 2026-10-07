import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import api from "../api/axiosInstance";
import { resolveMediaType } from "../utils/media";
import { removeItemFromList, restoreItemToList } from "../utils/listItems";
import { useListsStore } from "../stores/listsStore";

/** Backend path value for the rejected list (same style as lovedlist). */
export const REJECTED_LIST_TYPE = "rejectedlist";

/**
 * Map item media type to recommendations store key (movie | tv).
 */
function recommendationKey(mediaType) {
  return String(mediaType).toLowerCase() === "tv" ? "tv" : "movie";
}

/**
 * Optimistic reject with per-item rollback. Exported for unit tests (no renderHook in repo).
 */
export async function performRejectTitle(item, deps) {
  const {
    getListsState,
    setListsState,
    queryClient,
    putRejected,
    toastError,
  } = deps;
  const tmdbId = item?.id;
  if (tmdbId == null) {
    return;
  }
  const uiType = resolveMediaType(item);
  const mediaType = item?.mediaType ?? item?.media_type ?? uiType;
  const recKey = recommendationKey(mediaType);

  const state = getListsState();
  const recList = state.recommendations?.[recKey];
  const recResult = Array.isArray(recList)
    ? removeItemFromList(recList, tmdbId, mediaType)
    : { items: recList, removed: null };
  const watchResult = removeItemFromList(state.watchlist, tmdbId, mediaType);
  const lovedResult = removeItemFromList(state.lovedlist, tmdbId, mediaType);

  const similarRollbacks = [];
  for (const prefix of [["similar"], ["recommendationFiltered"]]) {
    for (const [queryKey, data] of queryClient.getQueriesData({
      queryKey: prefix,
    })) {
      if (!Array.isArray(data)) {
        continue;
      }
      const result = removeItemFromList(data, tmdbId, mediaType);
      if (result.removed) {
        similarRollbacks.push({ queryKey, removed: result.removed });
        queryClient.setQueryData(queryKey, result.items);
      }
    }
  }

  setListsState({
    recommendations: {
      ...state.recommendations,
      [recKey]: Array.isArray(recList) ? recResult.items : recList,
    },
    watchlist: watchResult.items,
    lovedlist: lovedResult.items,
  });

  try {
    await putRejected(uiType, tmdbId, REJECTED_LIST_TYPE);
    queryClient.invalidateQueries({ queryKey: ["rejected"] });
  } catch {
    const latest = getListsState();
    setListsState({
      recommendations: {
        ...latest.recommendations,
        [recKey]: restoreItemToList(
          Array.isArray(latest.recommendations?.[recKey])
            ? latest.recommendations[recKey]
            : [],
          recResult.removed,
        ),
      },
      watchlist: restoreItemToList(latest.watchlist, watchResult.removed),
      lovedlist: restoreItemToList(latest.lovedlist, lovedResult.removed),
    });
    for (const { queryKey, removed } of similarRollbacks) {
      const current = queryClient.getQueryData(queryKey);
      queryClient.setQueryData(
        queryKey,
        restoreItemToList(Array.isArray(current) ? current : [], removed),
      );
    }
    toastError("Could not hide this title. Please try again.");
  }
}

/**
 * Returns rejectTitle for Recommendation and Find similar cards.
 */
export default function useRejectTitle() {
  const queryClient = useQueryClient();

  return useCallback(
    (item) =>
      performRejectTitle(item, {
        getListsState: () => useListsStore.getState(),
        setListsState: (partial) => useListsStore.setState(partial),
        queryClient,
        putRejected: async (mediaType, movieId, listType) => {
          const res = await api.put(
            `/user/list/${mediaType}/${listType}/${movieId}`,
            null,
          );
          if (res.status !== 204) {
            throw new Error(`reject failed with status ${res.status}`);
          }
        },
        toastError: (message) => toast.error(message),
      }),
    [queryClient],
  );
}
