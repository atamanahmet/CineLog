import { beforeEach, describe, expect, it, vi } from "vitest";
import { createStore } from "zustand/vanilla";
import { persist } from "zustand/middleware";
import {
  emptyFilters,
  filtersAfterReset,
  migrateFilters,
  readPersistJson,
  resolveEffectiveFilters,
  sanitizeFilters,
} from "../lib/sanitizeFilters";
import { getPresetSort } from "../lib/pagePresets";
import { TEST_BOUNDS } from "../lib/testBounds";
import { migrateUserPrefs } from "./userPrefsStore";

const STORAGE_KEY = "cinelog-prefs";

function memoryStorage(seed = {}) {
  const data = { ...seed };
  return {
    getItem: (name) => readPersistJson(data[name] ?? null),
    setItem: (name, value) => {
      data[name] = JSON.stringify(value);
    },
    removeItem: (name) => {
      delete data[name];
    },
  };
}

function createPrefsStore(rawStorage) {
  return createStore(
    persist(
      (set, get) => ({
        mediaType: "movie",
        includeAdult: false,
        sortByPage: {
          discover: null,
          top: null,
          newReleases: null,
          search: null,
          recommendation: "match",
        },
        setMediaType: (mediaType) => set({ mediaType }),
        setIncludeAdult: (includeAdult) =>
          set({ includeAdult: Boolean(includeAdult) }),
        setSortForPage: (page, value) =>
          set((state) => ({
            sortByPage: { ...state.sortByPage, [page]: value },
          })),
        getEffectiveSort: (pageKey) => {
          const userSort = get().sortByPage[pageKey];
          if (userSort != null && userSort !== "") {
            return userSort;
          }
          return getPresetSort(pageKey);
        },
      }),
      {
        name: STORAGE_KEY,
        version: 5,
        storage: rawStorage,
        migrate: (persisted, version) => migrateUserPrefs(persisted, version),
        merge: (persisted, current) => ({
          ...current,
          ...persisted,
          sortByPage: { ...current.sortByPage, ...persisted?.sortByPage },
        }),
      },
    ),
  );
}

function waitHydrated(store, ms = 2000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("hydrate timeout")), ms);
    const finish = () => {
      clearTimeout(timer);
      resolve();
    };
    if (store.persist.hasHydrated()) {
      finish();
      return;
    }
    store.persist.onFinishHydration(finish);
  });
}

describe("sanitizeFilters null defaults", () => {
  it("returns empty shape for null and non-object input", () => {
    expect(sanitizeFilters(null, TEST_BOUNDS)).toEqual(emptyFilters(TEST_BOUNDS));
    expect(sanitizeFilters(undefined, TEST_BOUNDS)).toEqual(
      emptyFilters(TEST_BOUNDS),
    );
    expect(sanitizeFilters("bad", TEST_BOUNDS)).toEqual(
      emptyFilters(TEST_BOUNDS),
    );
  });

  it("falls back per field for wrong types", () => {
    const result = sanitizeFilters(
      {
        yearRange: "bad",
        rating: 7,
        languages: 1,
        adult: "yes",
        voteCount: "no",
      },
      TEST_BOUNDS,
    );
    expect(result).toEqual(emptyFilters(TEST_BOUNDS));
  });
});

describe("userPrefs migrate to v5", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("drops filters from version 4 and keeps other keys", async () => {
    const payload = JSON.stringify({
      state: {
        mediaType: "tv",
        profileTab: "Recommendation",
        recommendationView: "all",
        sortByPage: { discover: "popularity.desc", recommendation: "match" },
        filters: {
          genres: [28],
          yearRange: [1800, 2100],
          rating: [0, 10],
          languages: [],
          adult: true,
          voteCount: null,
          minRuntime: null,
          maxRuntime: null,
        },
        showAdultUnblurred: true,
      },
      version: 4,
    });
    const store = createPrefsStore(
      memoryStorage({ [STORAGE_KEY]: payload }),
    );
    await waitHydrated(store);
    const state = store.getState();
    expect(state.filters).toBeUndefined();
    expect(state.mediaType).toBe("tv");
    expect(state.profileTab).toBe("Recommendation");
    expect(state.recommendationView).toBe("all");
    expect(state.sortByPage.discover).toBe("popularity.desc");
    expect(state.showAdultUnblurred).toBe(true);
    expect(state.includeAdult).toBe(true);
  });

  it("garbage shape does not throw", () => {
    expect(() => migrateUserPrefs(null, 4)).not.toThrow();
    expect(() => migrateUserPrefs("bad", 3)).not.toThrow();
    expect(() => migrateUserPrefs({ filters: "nope", sortByPage: 1 }, 2)).not.toThrow();
    expect(() => migrateUserPrefs([1, 2], 1)).not.toThrow();
    const migrated = migrateUserPrefs({ filters: "nope", theme: "dark" }, 4);
    expect(migrated.filters).toBeUndefined();
    expect(migrated.theme).toBe("dark");
  });

  it("corrupt JSON hydrates without throw", async () => {
    const store = createPrefsStore(
      memoryStorage({ [STORAGE_KEY]: "{not-json" }),
    );
    await expect(waitHydrated(store)).resolves.toBeUndefined();
    expect(store.getState().mediaType).toBe("movie");
    expect(store.getState().filters).toBeUndefined();
  });
});

describe("resolveEffectiveFilters still works for presets", () => {
  it("Top effective voteCount comes from preset when user is empty", () => {
    expect(
      resolveEffectiveFilters(
        filtersAfterReset(TEST_BOUNDS),
        "top",
        "movie",
        TEST_BOUNDS,
      ).voteCount,
    ).toBe(1000);
  });

  it("Discover effective voteCount stays null", () => {
    expect(
      resolveEffectiveFilters(
        emptyFilters(TEST_BOUNDS),
        "discover",
        "movie",
        TEST_BOUNDS,
      ).voteCount,
    ).toBeNull();
  });

  it("Top effective sort comes from preset when user sort is null", () => {
    const store = createPrefsStore(memoryStorage());
    expect(store.getState().getEffectiveSort("top")).toBe(
      "vote_average.desc",
    );
  });

  it("leaves sort and media type alone when prefs change", () => {
    const store = createPrefsStore(memoryStorage());
    store.getState().setMediaType("tv");
    store.getState().setSortForPage("discover", "vote_average.desc");
    expect(store.getState().mediaType).toBe("tv");
    expect(store.getState().sortByPage.discover).toBe("vote_average.desc");
  });
});

describe("migrateFilters helper", () => {
  it("turns saved voteCount 500 into null", () => {
    const migrated = migrateFilters(
      { voteCount: 500, duration: [60, 180] },
      3,
    );
    expect(migrated.voteCount).toBeNull();
    expect(migrated.duration).toBeUndefined();
  });
});
