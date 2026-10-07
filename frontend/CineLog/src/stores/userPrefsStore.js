import { create } from "zustand";
import { persist } from "zustand/middleware";
import { migrateFilters, readPersistJson } from "../lib/sanitizeFilters";
import { getPresetSort } from "../lib/pagePresets";
import { mapDiscoverSortForMediaType } from "../lib/sortOptions";

const CATALOG_SORT_PAGES = [
  "discover",
  "top",
  "newReleases",
  "upcoming",
  "search",
];

export {
  emptyFilters,
  filtersAfterReset,
  filtersMatchPreset,
  resolveEffectiveFilters,
  sanitizeFilters,
  showsRuntimeFilter,
  showsVoteCountFilter,
} from "../lib/sanitizeFilters";

const DEFAULT_SORT_BY_PAGE = {
  discover: null,
  top: null,
  newReleases: null,
  upcoming: null,
  search: null,
  recommendation: "match",
};

const systemTheme = () =>
  typeof window !== "undefined" &&
  window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";

/**
 * Persisted user preferences. Filter memory lives in pageFiltersStore.
 */
const defaultPrefs = () => ({
  theme: systemTheme(),
  filterPanelOpen: false,
  mediaType: "movie",
  recommendationView: "movie",
  sortByPage: { ...DEFAULT_SORT_BY_PAGE },
  profileTab: "Watchlist",
  includeAdult: false,
  showAdultUnblurred: false,
});

/**
 * Persist storage that skips corrupt JSON instead of failing hydrate.
 */
function createPrefsStorage() {
  return {
    getItem: (name) => {
      if (typeof localStorage === "undefined") {
        return null;
      }
      return readPersistJson(localStorage.getItem(name));
    },
    setItem: (name, value) => {
      if (typeof localStorage === "undefined") {
        return;
      }
      localStorage.setItem(name, JSON.stringify(value));
    },
    removeItem: (name) => {
      if (typeof localStorage === "undefined") {
        return;
      }
      localStorage.removeItem(name);
    },
  };
}

/**
 * Migrate any older prefs shape to version 5 without filters.
 */
export function migrateUserPrefs(persisted, version) {
  if (!persisted || typeof persisted !== "object" || Array.isArray(persisted)) {
    return persisted;
  }
  const next = { ...persisted };
  try {
    if (typeof next.includeAdult !== "boolean") {
      const fromFilters =
        next.filters &&
        typeof next.filters === "object" &&
        typeof next.filters.adult === "boolean"
          ? next.filters.adult
          : false;
      next.includeAdult = fromFilters;
    }
    if (version < 2 && next.filters && typeof next.filters === "object") {
      delete next.filters.sort;
    }
    if (next.filters && typeof next.filters === "object") {
      migrateFilters(next.filters, version);
    }
  } catch {
    // Keep other keys even when old filter fields are garbage.
  }
  delete next.filters;
  return next;
}

export const useUserPrefsStore = create(
  persist(
    (set, get) => ({
      ...defaultPrefs(),

      resetPreferences: () => set(defaultPrefs()),

      setTheme: (theme) => set({ theme }),
      toggleTheme: () =>
        set((state) => ({ theme: state.theme === "dark" ? "light" : "dark" })),

      setFilterPanelOpen: (filterPanelOpen) => set({ filterPanelOpen }),

      setMediaType: (mediaType) =>
        set((state) => {
          const sortByPage = { ...state.sortByPage };
          for (const page of CATALOG_SORT_PAGES) {
            sortByPage[page] = mapDiscoverSortForMediaType(
              sortByPage[page],
              mediaType,
            );
          }
          return { mediaType, sortByPage };
        }),

      setRecommendationView: (recommendationView) =>
        set({ recommendationView }),

      setSortForPage: (page, value) =>
        set((state) => ({
          sortByPage: { ...state.sortByPage, [page]: value },
        })),

      /**
       * Effective sort for a catalog page: user sort, else preset sort.
       */
      getEffectiveSort: (pageKey) => {
        const userSort = get().sortByPage[pageKey];
        if (userSort != null && userSort !== "") {
          return userSort;
        }
        return getPresetSort(pageKey);
      },

      setProfileTab: (profileTab) => set({ profileTab }),

      setIncludeAdult: (includeAdult) => set({ includeAdult: Boolean(includeAdult) }),

      setShowAdultUnblurred: (showAdultUnblurred) => set({ showAdultUnblurred }),
    }),
    {
      name: "cinelog-prefs",
      version: 5,
      storage: createPrefsStorage(),
      migrate: (persisted, version) => migrateUserPrefs(persisted, version),
      merge: (persisted, current) => ({
        ...current,
        ...persisted,
        sortByPage: { ...current.sortByPage, ...persisted?.sortByPage },
      }),
    },
  ),
);
