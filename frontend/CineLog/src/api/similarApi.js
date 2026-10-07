import api from "./axiosInstance";
import { normalizeMediaItem } from "../utils/media";

/**
 * Ask the backend for titles similar to the given seeds.
 */
export async function fetchSimilarRecommendations(
  seeds,
  mediaType,
  genreFilter,
) {
  const body = {
    seeds: (seeds ?? []).map((seed) => ({
      tmdbId: seed.tmdbId,
      mediaType: seed.mediaType,
    })),
    mediaType,
  };
  const include = genreFilter?.include ?? [];
  const exclude = genreFilter?.exclude ?? [];
  if (include.length > 0) {
    body.genreInclude = include;
  }
  if (exclude.length > 0) {
    body.genreExclude = exclude;
  }
  const { data } = await api.post("/recommendation/similar", body);
  const rows = Array.isArray(data) ? data : [];
  return rows.map(normalizeMediaItem);
}

/**
 * Search one media type for the seed picker. Returns one TMDB page.
 */
export async function searchTitles(mediaType, query, page = 1, minVotes) {
  const path = mediaType === "TV" ? "/tv/search" : "/movie/search";
  const params = { query, page };
  if (minVotes != null) {
    params.minVotes = minVotes;
  }
  const { data } = await api.get(path, { params });
  let items = [];
  let totalPages = null;
  if (Array.isArray(data)) {
    items = data;
  } else if (data != null && Array.isArray(data.results)) {
    items = data.results;
    if (typeof data.totalPages === "number") {
      totalPages = data.totalPages;
    }
  }
  return {
    items: items.map((item) =>
      normalizeMediaItem({ ...item, mediaType }),
    ),
    totalPages,
  };
}
