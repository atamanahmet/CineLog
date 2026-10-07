/**
 * Remove one title by id and media type. Does not mutate the input array.
 */
export function removeItemFromList(items, tmdbId, mediaType) {
  if (!Array.isArray(items)) {
    return { items: items ?? [], removed: null };
  }
  const idNum = Number(tmdbId);
  const type = String(mediaType ?? "").toLowerCase();
  const index = items.findIndex(
    (item) =>
      Number(item?.id) === idNum &&
      String(item?.mediaType ?? item?.media_type ?? "").toLowerCase() === type,
  );
  if (index < 0) {
    return { items, removed: null };
  }
  const next = items.slice();
  const [item] = next.splice(index, 1);
  return { items: next, removed: { item, index } };
}

/**
 * Put a previously removed item back at its index. No-op when removed is null
 * or the item is already present.
 */
export function restoreItemToList(items, removed) {
  if (removed == null || removed.item == null) {
    return Array.isArray(items) ? items.slice() : [];
  }
  const list = Array.isArray(items) ? items.slice() : [];
  const idNum = Number(removed.item.id);
  const type = String(
    removed.item.mediaType ?? removed.item.media_type ?? "",
  ).toLowerCase();
  const already = list.some(
    (item) =>
      Number(item?.id) === idNum &&
      String(item?.mediaType ?? item?.media_type ?? "").toLowerCase() === type,
  );
  if (already) {
    return list;
  }
  const index = Math.max(0, Math.min(Number(removed.index) || 0, list.length));
  list.splice(index, 0, removed.item);
  return list;
}

/**
 * Drop titles that appear in any of the other lists (id + mediaType match).
 */
export function omitListedItems(items, ...lists) {
  if (!Array.isArray(items)) {
    return [];
  }
  const blocked = new Set();
  for (const list of lists) {
    if (!Array.isArray(list)) {
      continue;
    }
    for (const entry of list) {
      const id = Number(entry?.id);
      const type = String(
        entry?.mediaType ?? entry?.media_type ?? "",
      ).toLowerCase();
      if (!Number.isFinite(id) || !type) {
        continue;
      }
      blocked.add(`${id}:${type}`);
    }
  }
  return items.filter((item) => {
    const id = Number(item?.id);
    const type = String(
      item?.mediaType ?? item?.media_type ?? "",
    ).toLowerCase();
    return !blocked.has(`${id}:${type}`);
  });
}
