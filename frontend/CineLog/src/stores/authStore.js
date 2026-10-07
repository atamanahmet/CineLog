import { create } from "zustand";
import api from "../api/axiosInstance";

export const useAuthStore = create((set) => ({
  user: null,
  profilePhotoUrl: null,

  login: async (username, password) => {
    const res = await api.post(
      "/auth/login",
      { username, password },
      { withCredentials: true },
    );
    if (res.status === 200) {
      set({ user: res.data });
    }
    return res;
  },

  register: async (username, password) => {
    return api.post(
      "/auth/register",
      { username, password },
      { withCredentials: true },
    );
  },

  logout: async () => {
    try {
      await api.post("/auth/logout", {});
    } finally {
      set({ user: null, profilePhotoUrl: null });
    }
  },

  fetchUser: async () => {
    try {
      const res = await api.get("/auth/me");
      set({ user: res.data?.username ?? res.data });
    } catch (err) {
      if (err.response?.status !== 401) {
        console.log("Error: " + err);
      }
      set({ user: null, profilePhotoUrl: null });
    }
  },

  setProfilePhotoUrl: (url) => {
    set({ profilePhotoUrl: url || null });
  },

  uploadPhoto: async (file) => {
    if (file == null) {
      return;
    }
    const formData = new FormData();
    formData.append("profilePicture", file);
    const res = await api.post("/user/upload", formData, {
      headers: { "content-type": "multipart/form-data" },
    });
    const url = res.data?.profilePictureUrl;
    if (url) {
      set({ profilePhotoUrl: url });
    }
    return res;
  },
}));
