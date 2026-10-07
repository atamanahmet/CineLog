/**
 * TMDB discover only supports with_original_language (include one code).
 * Send a hint only when exactly one language is included. Exclude never sent.
 */
export function languageServerHint(languageFilter) {
  const include = languageFilter?.include;
  if (!Array.isArray(include) || include.length !== 1) {
    return null;
  }
  const code = include[0];
  if (code == null || code === "") {
    return null;
  }
  return String(code);
}
