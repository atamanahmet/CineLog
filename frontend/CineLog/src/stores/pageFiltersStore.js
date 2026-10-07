import { create } from "zustand";
import { emptyFiltersSoft } from "../lib/sanitizeFilters";
import { PAGE_MEMORY_TTL_MS } from "../lib/pageMemory";
import { emptyTriState } from "../lib/triStateFilter";
import { useUserPrefsStore } from "./userPrefsStore";

const DISCOVER_SCOPE = "discover";

/**
 * In-memory filter state per page scope. Not persisted.
 */
export const usePageFiltersStore = create((set, get) => ({
  entries: {},

  /**
   * Stored filters for a scope, or null. Clears genre lists when media type differs.
   * Language filter is kept (ISO codes mean the same for movie and TV).
   */
  getFilters: (scopeKey, mediaType) => {
    if (scopeKey == null) {
      return null;
    }
    const entry = get().entries[scopeKey];
    if (!entry) {
      return null;
    }
    if (entry.mediaType !== mediaType) {
      return {
        ...entry.filters,
        genreFilter: emptyTriState(),
        // Language codes are the same for movie and TV; keep languageFilter.
      };
    }
    return entry.filters;
  },

  /**
   * Store filters for a scope and mark the page as active.
   */
  setFilters: (scopeKey, filters, mediaType) => {
    if (scopeKey == null) {
      return;
    }
    set((state) => ({
      entries: {
        ...state.entries,
        [scopeKey]: { filters, mediaType, leftAt: null },
      },
    }));
  },

  /**
   * Open Discover for one included genre. Clears other genre picks. Keeps language and other filters.
   */
  applyGenreEntry: (mediaType, genreId) => {
    if (mediaType !== "movie" && mediaType !== "tv") {
      return;
    }
    const id = Number(genreId);
    if (!Number.isFinite(id)) {
      return;
    }
    useUserPrefsStore.getState().setMediaType(mediaType);
    set((state) => {
      const prev = state.entries[DISCOVER_SCOPE]?.filters;
      const base = prev != null ? { ...prev } : emptyFiltersSoft();
      return {
        entries: {
          ...state.entries,
          [DISCOVER_SCOPE]: {
            filters: {
              ...base,
              genreFilter: { include: [id], exclude: [] },
            },
            mediaType,
            leftAt: null,
          },
        },
      };
    });
  },

  /**
   * Drop a scope entry so the page preset applies again.
   */
  resetFilters: (scopeKey) => {
    if (scopeKey == null) {
      return;
    }
    set((state) => {
      if (!(scopeKey in state.entries)) {
        return state;
      }
      const entries = { ...state.entries };
      delete entries[scopeKey];
      return { entries };
    });
  },

  /**
   * Record when the user left a page scope.
   */
  markLeft: (scopeKey, now) => {
    if (scopeKey == null) {
      return;
    }
    set((state) => {
      const entry = state.entries[scopeKey];
      if (!entry) {
        return state;
      }
      return {
        entries: {
          ...state.entries,
          [scopeKey]: { ...entry, leftAt: now },
        },
      };
    });
  },

  /**
   * Enter a scope. Drop the entry when away longer than the page memory TTL.
   */
  markEntered: (scopeKey, now) => {
    if (scopeKey == null) {
      return;
    }
    set((state) => {
      const entry = state.entries[scopeKey];
      if (!entry) {
        return state;
      }
      if (
        entry.leftAt != null &&
        now - entry.leftAt > PAGE_MEMORY_TTL_MS
      ) {
        const entries = { ...state.entries };
        delete entries[scopeKey];
        return { entries };
      }
      return {
        entries: {
          ...state.entries,
          [scopeKey]: { ...entry, leftAt: null },
        },
      };
    });
  },

  /**
   * Clear every page filter entry (logout).
   */
  clearAll: () => set({ entries: {} }),
}));
