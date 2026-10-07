import { useQuery } from "@tanstack/react-query";
import api from "../api/axiosInstance";

/**
 * Genre lists change rarely, cache long so switching mediaType doesn't refetch every time.
 */
const GENRE_STALE_TIME_MS = 24 * 60 * 60 * 1000;

/**
 * Genre list for one media type, fetched from the backend genre endpoint.
 */
export default function useGenreOptions(mediaType) {
  const query = useQuery({
    queryKey: ["genres", mediaType],
    queryFn: async () => {
      const res = await api.get(`/${mediaType}/genres`);
      return res.data;
    },
    staleTime: GENRE_STALE_TIME_MS,
    gcTime: GENRE_STALE_TIME_MS,
  });

  return {
    genres: query.data ?? [],
    isLoading: query.isLoading,
    isError: query.isError,
  };
}
