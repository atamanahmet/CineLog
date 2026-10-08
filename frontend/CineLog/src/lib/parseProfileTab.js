/**
 * Resolve a profile tab query value against known keys.
 * Unknown or missing values return the fallback.
 */
export function parseProfileTab(value, validKeys, fallbackKey) {
  if (value == null || value === "") {
    return fallbackKey;
  }
  if (validKeys.includes(value)) {
    return value;
  }
  return fallbackKey;
}
