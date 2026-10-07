import { useQuery } from "@tanstack/react-query";
import api from "../api/axiosInstance";

/**
 * Public backend feature flags. Cached for the whole session.
 */
export default function useSiteConfig() {
  return useQuery({
    queryKey: ["site-config"],
    queryFn: async () => (await api.get("/config")).data,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: 1,
  });
}
