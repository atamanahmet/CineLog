import api from "./axiosInstance";
import { normalizeMediaItem } from "../utils/media";

/**
 * Lazy-load one user list by lowercase path value (e.g. rejectedlist).
 */
export async function fetchUserList(listType) {
  const { data } = await api.get(`/user/list/${listType}`);
  const rows = Array.isArray(data) ? data : [];
  return rows.map(normalizeMediaItem);
}

/**
 * Remove one list entry. Same path shape as store removeFromList, no archive refresh.
 */
export async function removeListEntry(mediaType, listType, id) {
  const res = await api.delete(`/user/list/${mediaType}/${listType}/${id}`);
  if (res.status !== 204) {
    throw new Error(`remove failed with status ${res.status}`);
  }
}
