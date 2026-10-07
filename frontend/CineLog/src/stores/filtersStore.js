import { create } from "zustand";

/**
 * Session-only search state. Persisted selections live in userPrefsStore.
 */
export const useFiltersStore = create((set) => ({
  searchQuery: "",

  setSearchQuery: (searchQuery) => set({ searchQuery }),

  /**
   * Record the search term for SearchPage / React Query. Returns the term, or null if empty.
   */
  search: (searchQuery) => {
    if (searchQuery == null || String(searchQuery).trim() === "") {
      return null;
    }
    const term = String(searchQuery).trim();
    set({ searchQuery: term });
    return term;
  },
}));
