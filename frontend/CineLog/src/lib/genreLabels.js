/**
 * Canonical movie genre ids plus TV-only chips for the genre filter UI.
 */
export const CANONICAL_GENRE_CHIPS = [
  { id: 28, name: "Action" },
  { id: 12, name: "Adventure" },
  { id: 16, name: "Animation" },
  { id: 35, name: "Comedy" },
  { id: 80, name: "Crime" },
  { id: 99, name: "Documentary" },
  { id: 18, name: "Drama" },
  { id: 10751, name: "Family" },
  { id: 14, name: "Fantasy" },
  { id: 36, name: "History" },
  { id: 27, name: "Horror" },
  { id: 10402, name: "Music" },
  { id: 9648, name: "Mystery" },
  { id: 10749, name: "Romance" },
  { id: 878, name: "Science Fiction" },
  { id: 10770, name: "TV Movie" },
  { id: 53, name: "Thriller" },
  { id: 10752, name: "War" },
  { id: 37, name: "Western" },
  { id: 10763, name: "News" },
  { id: 10764, name: "Reality" },
  { id: 10766, name: "Soap" },
  { id: 10767, name: "Talk" },
];

/** TV-only chip ids. Shown in TV and Mixed views only. */
export const TV_ONLY_GENRE_IDS = new Set([10763, 10764, 10766, 10767]);

const genreMap = Object.fromEntries(
  CANONICAL_GENRE_CHIPS.map((chip) => [chip.id, chip.name]),
);

// Keep legacy TV expand labels for display of stored genre_ids on cards.
Object.assign(genreMap, {
  10759: "Action & Adventure",
  10762: "Kids",
  10765: "Sci-Fi & Fantasy",
  10768: "War & Politics",
});

/**
 * Genre names for known ids. Unknown ids are left out.
 */
export function genreLabels(ids) {
  if (!Array.isArray(ids)) {
    return [];
  }
  const labels = [];
  for (const id of ids) {
    const name = genreMap[id];
    if (name) {
      labels.push(name);
    }
  }
  return labels;
}

/**
 * Known genre id and name pairs. Unknown ids are left out.
 */
export function labeledGenres(ids) {
  if (!Array.isArray(ids)) {
    return [];
  }
  const out = [];
  for (const id of ids) {
    const name = genreMap[id];
    if (name) {
      out.push({ id, name });
    }
  }
  return out;
}

/**
 * Chips for the current media view. TV-only chips only in tv and Mixed (all).
 */
export function genreChipsForView(view) {
  if (view === "movie") {
    return CANONICAL_GENRE_CHIPS.filter((chip) => !TV_ONLY_GENRE_IDS.has(chip.id));
  }
  return CANONICAL_GENRE_CHIPS;
}
