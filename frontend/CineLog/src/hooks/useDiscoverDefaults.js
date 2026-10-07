import { useQuery } from "@tanstack/react-query";
import api from "../api/axiosInstance";
import { mapDiscoverDefaults } from "../lib/mapDiscoverDefaults";

/**
 * Fetch and map discover defaults from the backend.
 */
export async function fetchDiscoverDefaults() {
  const res = await api.get("/discover/defaults");
  return mapDiscoverDefaults(res.data);
}

/**
 * Backend discover defaults and bounds. One source for filter ranges.
 */
export default function useDiscoverDefaults() {
  return useQuery({
    queryKey: ["discover-defaults"],
    queryFn: fetchDiscoverDefaults,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: true,
  });
}
