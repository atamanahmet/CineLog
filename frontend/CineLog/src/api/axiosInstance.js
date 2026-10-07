import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
  paramsSerializer: {
    indexes: null,
  },
});

const SKIP_REFRESH_PATHS = [
  "/auth/login",
  "/auth/register",
  "/auth/refresh",
  "/auth/logout",
];

let refreshPromise = null;
let clearLoggedInUser = null;

/**
 * Register callback that clears logged-in user state without calling logout.
 */
export const setClearLoggedInUser = (fn) => {
  clearLoggedInUser = fn;
};

const shouldSkipRefresh = (url = "") =>
  SKIP_REFRESH_PATHS.some((path) => url.includes(path));

const refreshSession = () => {
  if (!refreshPromise) {
    refreshPromise = api
      .post("/auth/refresh")
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      shouldSkipRefresh(originalRequest.url)
    ) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      await refreshSession();
      return api(originalRequest);
    } catch (refreshError) {
      clearLoggedInUser?.();
      return Promise.reject(refreshError);
    }
  },
);

export default api;
