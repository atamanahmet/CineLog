/**
 * Flatten search pages, keep first mediaType+id, drop items without a poster.
 */
export function flattenSeedSearchPages(pages) {
  const seen = new Set();
  const out = [];
  for (const page of pages ?? []) {
    const items = Array.isArray(page?.items) ? page.items : [];
    for (const item of items) {
      if (item == null || item.id == null || item.mediaType == null) {
        continue;
      }
      const key = `${item.mediaType}:${item.id}`;
      if (seen.has(key)) {
        continue;
      }
      seen.add(key);
      if (!item.posterPath) {
        continue;
      }
      out.push(item);
    }
  }
  return out;
}
