/** Preset filter keys shared by Recommendation and Find similar. */
export const REC_FILTER_BASE_KEYS = ["yearRange", "rating"];

/**
 * Active client filter keys for a rec or similar view. Genres are server-side.
 */
export function recFilterActiveKeys() {
  return [...REC_FILTER_BASE_KEYS];
}
