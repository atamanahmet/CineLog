import { create } from "zustand";

/**
 * Shared open/mode state for the login/register modal.
 * Any component can trigger it without a route change.
 */
export const useAuthModalStore = create((set) => ({
  isOpen: false,
  mode: "login",
  openModal: (mode = "login") => set({ isOpen: true, mode }),
  closeModal: () => set({ isOpen: false }),
  setMode: (mode) => set({ mode }),
}));
