import { create } from "zustand";

/** Hard cap on seed titles for find-similar. */
export const MAX_SEEDS = 10;

const initialState = {
  seeds: [],
  view: "movie",
};

/**
 * Session-only find-similar seeds and media view. Never persisted.
 */
export const useSimilarStore = create((set, get) => ({
  ...initialState,

  /**
   * Add one seed when under the cap and not already present.
   */
  addSeed: (seed) => {
    if (seed == null || seed.tmdbId == null || seed.mediaType == null) {
      return;
    }
    const { seeds } = get();
    if (seeds.length >= MAX_SEEDS) {
      return;
    }
    const exists = seeds.some(
      (item) =>
        item.tmdbId === seed.tmdbId && item.mediaType === seed.mediaType,
    );
    if (exists) {
      return;
    }
    set({ seeds: [...seeds, seed] });
  },

  /**
   * Remove one seed by tmdbId and mediaType.
   */
  removeSeed: (tmdbId, mediaType) => {
    set((state) => ({
      seeds: state.seeds.filter(
        (item) => !(item.tmdbId === tmdbId && item.mediaType === mediaType),
      ),
    }));
  },

  /**
   * Drop every seed at once.
   */
  clearSeeds: () => set({ seeds: [] }),

  /**
   * Set the results media view (movie, tv, or all).
   */
  setView: (view) => set({ view }),

  /**
   * Restore initial empty state.
   */
  reset: () => set({ seeds: [], view: "movie" }),
}));
