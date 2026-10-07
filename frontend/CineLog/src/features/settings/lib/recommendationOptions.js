export const REC_SCOPE_OPTIONS = [
  { value: "ALL", label: "Movies & TV" },
  { value: "MOVIE", label: "Movies" },
  { value: "TV", label: "TV Shows" },
];

export const REC_MAX_RESULTS_PRESETS = [10, 25, 50, 100];

export const REC_MIN_SCORE = { min: 0.1, max: 0.9, step: 0.05 };

/**
 * Build max-results options from presets and the server cap.
 */
export function buildMaxResultsOptions(maxResultsLimit) {
  const limit = Number(maxResultsLimit);
  if (!Number.isFinite(limit)) {
    return [...REC_MAX_RESULTS_PRESETS];
  }
  const options = REC_MAX_RESULTS_PRESETS.filter((n) => n <= limit);
  if (!options.includes(limit)) {
    options.push(limit);
  }
  return options;
}

/**
 * Human label for a similarity threshold.
 */
export function strictnessLabel(minScore) {
  if (minScore < 0.3) {
    return "Loose";
  }
  if (minScore < 0.55) {
    return "Balanced";
  }
  return "Strict";
}
