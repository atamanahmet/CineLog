/**
 * US long date for display, or null when input is missing or invalid.
 */
export function formatDisplayDate(dateString) {
  if (dateString == null || dateString === "") {
    return null;
  }
  const d = new Date(dateString);
  if (Number.isNaN(d.getTime())) {
    return null;
  }
  return d.toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

/**
 * Calendar year from an ISO date string, or null.
 */
export function yearFromDate(dateString) {
  if (dateString == null || dateString === "") {
    return null;
  }
  const d = new Date(dateString);
  if (Number.isNaN(d.getTime())) {
    return null;
  }
  return d.getFullYear();
}
