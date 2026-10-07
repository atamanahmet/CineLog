/**
 * Source of truth: embedding-script/genre_vocab.py (TV_EXPAND).
 * TV composite ids expand onto canonical movie ids; all other ids stay as-is.
 */
export const TV_EXPAND = {
  10759: [28, 12],
  10765: [878, 14],
  10768: [10752],
  10762: [10751],
};

/**
 * Same rule as recommendation-engine GENRE_INCLUDE_MODE.
 * "all" = item must carry every included id.
 */
export const GENRE_INCLUDE_MODE = "all";

/**
 * Canonical genre ids for one media item. Deduped, unsorted order from first seen.
 */
export function toCanonicalGenreIds(item) {
  const raw = item?.genreIds;
  if (!Array.isArray(raw) || raw.length === 0) {
    return [];
  }
  const mediaType = String(item?.mediaType ?? "").toUpperCase();
  const out = [];
  const seen = new Set();
  for (const value of raw) {
    const id = Number(value);
    if (!Number.isFinite(id)) {
      continue;
    }
    const expanded =
      mediaType === "TV" && TV_EXPAND[id] != null ? TV_EXPAND[id] : [id];
    for (const canon of expanded) {
      if (seen.has(canon)) {
        continue;
      }
      seen.add(canon);
      out.push(canon);
    }
  }
  return out;
}
