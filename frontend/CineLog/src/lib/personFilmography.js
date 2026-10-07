import { yearFromDate } from "./displayDate";

/** TMDB genre ids treated as appearance formats when roles are only self-like. */
export const APPEARANCE_GENRE_IDS = new Set([10767, 10763, 10764]);

const SELF_LIKE_LABEL = /^(self|himself|herself)\b/i;

/**
 * Normalize A1 person API payload into page state.
 */
export function mapPersonDetail(payload) {
  if (payload == null || typeof payload !== "object") {
    return { person: null, credits: [] };
  }
  const rawPerson = payload.person;
  const person =
    rawPerson == null
      ? null
      : {
          id: rawPerson.id ?? null,
          name: rawPerson.name ?? "",
          biography: blankToNull(rawPerson.biography),
          birthday: blankToNull(rawPerson.birthday),
          deathday: blankToNull(rawPerson.deathday),
          placeOfBirth: blankToNull(rawPerson.placeOfBirth),
          profilePath: blankToNull(rawPerson.profilePath),
          knownForDepartment: blankToNull(rawPerson.knownForDepartment),
          alsoKnownAs: Array.isArray(rawPerson.alsoKnownAs)
            ? rawPerson.alsoKnownAs.filter(Boolean)
            : [],
        };
  const credits = Array.isArray(payload.credits)
    ? payload.credits.map(mapCredit).filter(Boolean)
    : [];
  return { person, credits };
}

/**
 * Keep credits matching All / Movies / TV tab.
 */
export function filterByMediaType(credits, tab) {
  if (!Array.isArray(credits)) {
    return [];
  }
  if (tab == null || tab === "all") {
    return credits;
  }
  return credits.filter((c) => c.mediaType === tab);
}

/**
 * Unique departments present in credit roles, stable first-seen order.
 * Cast roles count as Acting; crew uses role.department.
 */
export function collectDepartments(credits) {
  if (!Array.isArray(credits)) {
    return [];
  }
  const seen = new Set();
  const out = [];
  for (const credit of credits) {
    for (const dept of roleDepartments(credit)) {
      if (!seen.has(dept)) {
        seen.add(dept);
        out.push(dept);
      }
    }
  }
  return out;
}

/**
 * Credits that have at least one role in the given department.
 */
export function filterByDepartment(credits, department) {
  if (!Array.isArray(credits) || department == null || department === "") {
    return Array.isArray(credits) ? credits : [];
  }
  return credits.filter((credit) => roleDepartments(credit).includes(department));
}

/**
 * Prefer known-for when it exists in the list; else first department.
 */
export function resolveDefaultDepartment(departments, knownForDepartment) {
  if (!Array.isArray(departments) || departments.length === 0) {
    return null;
  }
  if (
    knownForDepartment != null &&
    knownForDepartment !== "" &&
    departments.includes(knownForDepartment)
  ) {
    return knownForDepartment;
  }
  return departments[0];
}

/**
 * True when credit is talk/news/reality and every role is self-like.
 * Crew roles are never self-like. Scripted titles (no appearance genres) stay.
 */
export function isAppearanceCredit(credit) {
  if (credit == null) {
    return false;
  }
  const genreIds = Array.isArray(credit.genreIds) ? credit.genreIds : [];
  const hasAppearanceGenre = genreIds.some((id) => APPEARANCE_GENRE_IDS.has(id));
  if (!hasAppearanceGenre) {
    return false;
  }
  const roles = Array.isArray(credit.roles) ? credit.roles : [];
  if (roles.length === 0) {
    return true;
  }
  return roles.every(isSelfLikeRole);
}

/**
 * When showAppearances is false, drop appearance credits.
 */
export function filterAppearances(credits, showAppearances) {
  if (!Array.isArray(credits)) {
    return [];
  }
  if (showAppearances) {
    return credits;
  }
  return credits.filter((credit) => !isAppearanceCredit(credit));
}

/**
 * Group by year. Undated first as Upcoming / TBA, then newest year first.
 * Encounter order preserved inside each group.
 */
export function groupCreditsByYear(credits) {
  if (!Array.isArray(credits) || credits.length === 0) {
    return [];
  }
  const undated = [];
  const byYear = new Map();
  for (const credit of credits) {
    const year = yearFromDate(credit.date);
    if (year == null) {
      undated.push(credit);
      continue;
    }
    if (!byYear.has(year)) {
      byYear.set(year, []);
    }
    byYear.get(year).push(credit);
  }
  const groups = [];
  if (undated.length > 0) {
    groups.push({
      yearKey: null,
      label: "Upcoming / TBA",
      credits: undated,
    });
  }
  const years = [...byYear.keys()].sort((a, b) => b - a);
  for (const year of years) {
    groups.push({
      yearKey: year,
      label: String(year),
      credits: byYear.get(year),
    });
  }
  return groups;
}

/**
 * Pipeline: media tab → department → appearances → year groups.
 */
export function selectFilmographyGroups(
  credits,
  { mediaTab, department, showAppearances },
) {
  const byMedia = filterByMediaType(credits, mediaTab);
  const byDept = filterByDepartment(byMedia, department);
  const visible = filterAppearances(byDept, showAppearances);
  return groupCreditsByYear(visible);
}

/**
 * Department options from credits after media-type filter.
 */
export function departmentsForMedia(credits, mediaTab) {
  return collectDepartments(filterByMediaType(credits, mediaTab));
}

function mapCredit(raw) {
  if (raw == null || typeof raw !== "object") {
    return null;
  }
  const title = blankToNull(raw.title);
  if (title == null) {
    return null;
  }
  return {
    mediaType: raw.mediaType === "tv" ? "tv" : "movie",
    tmdbId: raw.tmdbId ?? null,
    title,
    posterPath: blankToNull(raw.posterPath),
    date: blankToNull(raw.date),
    voteAverage:
      typeof raw.voteAverage === "number" ? raw.voteAverage : null,
    genreIds: Array.isArray(raw.genreIds) ? raw.genreIds.filter(Number.isFinite) : [],
    roles: Array.isArray(raw.roles) ? raw.roles.map(mapRole).filter(Boolean) : [],
  };
}

function mapRole(raw) {
  if (raw == null || typeof raw !== "object") {
    return null;
  }
  const kind = raw.kind === "crew" ? "crew" : "cast";
  return {
    kind,
    label: blankToNull(raw.label),
    department: kind === "crew" ? blankToNull(raw.department) : null,
    episodeCount:
      typeof raw.episodeCount === "number" ? raw.episodeCount : null,
  };
}

function roleDepartments(credit) {
  const roles = Array.isArray(credit?.roles) ? credit.roles : [];
  const out = [];
  for (const role of roles) {
    if (role.kind === "cast") {
      out.push("Acting");
    } else if (role.department) {
      out.push(role.department);
    }
  }
  return out;
}

function isSelfLikeRole(role) {
  if (role == null || role.kind === "crew") {
    return false;
  }
  const label = role.label;
  if (label == null || label === "") {
    return true;
  }
  return SELF_LIKE_LABEL.test(label.trim());
}

function blankToNull(value) {
  if (value == null) {
    return null;
  }
  if (typeof value === "string" && value.trim() === "") {
    return null;
  }
  return value;
}
