/**
 * Empty include/exclude tri-state filter.
 */
export function emptyTriState() {
  return { include: [], exclude: [] };
}

/**
 * Count of selected include plus exclude values.
 */
export function countActive(filter) {
  return (filter?.include?.length ?? 0) + (filter?.exclude?.length ?? 0);
}

/**
 * True when either list has at least one value.
 */
export function isTriStateActive(filter) {
  return countActive(filter) > 0;
}

/**
 * Sorted key for one value list. Order of clicks does not change the key.
 * Works with numbers and strings.
 */
export function sortedValuesKey(values) {
  if (!Array.isArray(values) || values.length === 0) {
    return "";
  }
  return [...values]
    .filter((v) => v != null && v !== "")
    .map((v) => (typeof v === "number" ? v : String(v)))
    .sort((a, b) => {
      if (typeof a === "number" && typeof b === "number") {
        return a - b;
      }
      return String(a).localeCompare(String(b), undefined, { numeric: true });
    })
    .join(",");
}

/**
 * Stable key for a tri-state filter. Order of clicks does not change the key.
 */
export function filterKey(filter) {
  return `${sortedValuesKey(filter?.include)}|${sortedValuesKey(filter?.exclude)}`;
}

/**
 * Cycle one value: off → include → exclude → off.
 */
export function cycleFilterValue(filter, value) {
  const include = Array.isArray(filter?.include) ? filter.include : [];
  const exclude = Array.isArray(filter?.exclude) ? filter.exclude : [];
  if (include.includes(value)) {
    return {
      include: include.filter((v) => v !== value),
      exclude: [...exclude, value],
    };
  }
  if (exclude.includes(value)) {
    return {
      include: [...include],
      exclude: exclude.filter((v) => v !== value),
    };
  }
  return {
    include: [...include, value],
    exclude: [...exclude],
  };
}

/**
 * Sanitize tri-state filter. Invalid shape becomes empty.
 * Accepts positive integers and non-empty strings.
 */
export function sanitizeTriState(raw) {
  if (raw == null || typeof raw !== "object" || Array.isArray(raw)) {
    return emptyTriState();
  }
  const include = sanitizeValueList(raw.include);
  const exclude = sanitizeValueList(raw.exclude);
  const excludeSet = new Set(exclude);
  return {
    include: include.filter((v) => !excludeSet.has(v)),
    exclude,
  };
}

/**
 * Item original-language values for tri-state apply (0 or 1 code).
 */
export function toLanguageValues(item) {
  const code = item?.originalLanguage;
  if (code == null || code === "") {
    return [];
  }
  return [code];
}

/**
 * Client tri-state filter. Exclude = ANY match drops.
 * Include = "all" (every value) or "any" (at least one).
 * No values on item: pass exclude, fail include.
 */
export function applyTriStateFilter(
  items,
  filter,
  getValues,
  includeMode = "all",
) {
  if (!Array.isArray(items)) {
    return [];
  }
  const include = filter?.include ?? [];
  const exclude = filter?.exclude ?? [];
  if (include.length === 0 && exclude.length === 0) {
    return items.slice();
  }
  return items.filter((item) => {
    const values = getValues(item) ?? [];
    if (exclude.length > 0 && values.some((v) => exclude.includes(v))) {
      return false;
    }
    if (include.length > 0) {
      if (values.length === 0) {
        return false;
      }
      if (includeMode === "any") {
        if (!include.some((v) => values.includes(v))) {
          return false;
        }
      } else if (!include.every((v) => values.includes(v))) {
        return false;
      }
    }
    return true;
  });
}

function sanitizeValueList(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  const out = [];
  const seen = new Set();
  for (const item of value) {
    if (typeof item === "number") {
      if (!Number.isInteger(item) || item < 1 || seen.has(item)) {
        continue;
      }
      seen.add(item);
      out.push(item);
      continue;
    }
    if (typeof item === "string") {
      const trimmed = item.trim();
      if (!trimmed) {
        continue;
      }
      const asNum = Number(trimmed);
      if (
        Number.isInteger(asNum) &&
        asNum >= 1 &&
        String(asNum) === trimmed
      ) {
        if (seen.has(asNum)) {
          continue;
        }
        seen.add(asNum);
        out.push(asNum);
        continue;
      }
      if (seen.has(trimmed)) {
        continue;
      }
      seen.add(trimmed);
      out.push(trimmed);
    }
  }
  return out;
}
