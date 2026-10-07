import { create } from "zustand";
import api from "../api/axiosInstance";
import { useAuthStore } from "./authStore";
import { normalizeMediaItem } from "../utils/media";

/**
 * Normalize one list payload into canonical media items.
 */
function normalizeList(raw) {
  if (!raw) {
    return [];
  }
  const arr = Array.isArray(raw) ? raw : [];
  return arr.map(normalizeMediaItem);
}

/**
 * Map UI mediaType (movie/tv) to backend enum.
 */
function toApiMediaType(mediaType) {
  return mediaType === "tv" ? "TV" : "MOVIE";
}

/**
 * Normalize UI mediaType to movie or tv.
 */
function toUiMediaType(mediaType) {
  return mediaType === "tv" ? "tv" : "movie";
}

/**
 * True when list contains item with matching id + mediaType.
 */
export function isInList(list, id, mediaType) {
  if (!list || id == null || !mediaType) {
    return false;
  }
  const idNum = Number(id);
  const type = String(mediaType).toLowerCase();
  return list.some(
    (item) =>
      Number(item.id) === idNum &&
      String(item.mediaType ?? "").toLowerCase() === type,
  );
}

const emptyRecommendations = () => ({ movie: null, tv: null });
const emptyRecommendationErrors = () => ({ movie: false, tv: false });
const emptyRecommendationLoading = () => ({ movie: false, tv: false });

export const useListsStore = create((set, get) => ({
  watchlist: [],
  watchedlist: [],
  lovedlist: [],
  listsReady: false,
  recommendations: emptyRecommendations(),
  recommendationErrors: emptyRecommendationErrors(),
  recommendationLoading: emptyRecommendationLoading(),

  /**
   * Clear all list and recommendation state (logout / session end).
   */
  resetLists: () =>
    set({
      watchlist: [],
      watchedlist: [],
      lovedlist: [],
      listsReady: false,
      recommendations: emptyRecommendations(),
      recommendationErrors: emptyRecommendationErrors(),
      recommendationLoading: emptyRecommendationLoading(),
    }),

  /**
   * Clear both recommendation type caches so the next fetch is a cold load.
   */
  clearRecommendations: () =>
    set({
      recommendations: emptyRecommendations(),
      recommendationErrors: emptyRecommendationErrors(),
      recommendationLoading: emptyRecommendationLoading(),
    }),

  getWatchList: async () => {
    try {
      const res = await api.get("/user/lists");
      set({
        watchlist: normalizeList(res.data.watchlist),
        watchedlist: normalizeList(res.data.watchedlist),
        lovedlist: normalizeList(res.data.lovedlist),
        listsReady: true,
      });
      useAuthStore.getState().setProfilePhotoUrl(res.data.profilePictureUrl);
    } catch (err) {
      console.log(err);
      set({ listsReady: true });
    }
  },

  addToList: async (mediaType, movieId, listType) => {
    if (!movieId) {
      return;
    }
    try {
      const res = await api.put(
        `/user/list/${mediaType}/${listType}/${movieId}`,
        null,
      );
      if (res.status === 204) {
        await get().getWatchList();
      }
    } catch (err) {
      console.error("Backend error:", err);
    }
  },

  removeFromList: async (mediaType, movieId, listType) => {
    if (!movieId) {
      return;
    }
    try {
      const res = await api.delete(
        `/user/list/${mediaType}/${listType}/${movieId}`,
      );
      if (res.status === 204) {
        await get().getWatchList();
      }
    } catch (err) {
      console.error("Backend error:", err);
    }
  },

  /**
   * Fetch recommendations for one media type. Keeps prior list while refetching.
   */
  getRecommendation: async (mediaType) => {
    const key = toUiMediaType(mediaType);
    set((state) => ({
      recommendationErrors: { ...state.recommendationErrors, [key]: false },
      recommendationLoading: { ...state.recommendationLoading, [key]: true },
    }));
    try {
      const res = await api.get(`/user/recommendation`, {
        params: { mediaType: toApiMediaType(key) },
      });
      set((state) => ({
        recommendations: {
          ...state.recommendations,
          [key]: normalizeList(res.data),
        },
      }));
    } catch (err) {
      if (err.response?.status === 502) {
        set((state) => ({
          recommendationErrors: {
            ...state.recommendationErrors,
            [key]: true,
          },
        }));
        return;
      }
      console.log(err);
    } finally {
      set((state) => ({
        recommendationLoading: {
          ...state.recommendationLoading,
          [key]: false,
        },
      }));
    }
  },

  /**
   * Fetch movie and TV recommendations in parallel; one failure does not block the other.
   */
  getAllRecommendations: async () => {
    await Promise.allSettled([
      get().getRecommendation("movie"),
      get().getRecommendation("tv"),
    ]);
  },
}));
