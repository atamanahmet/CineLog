import { formatDisplayDate } from "./displayDate";

/**
 * Whole years between two date strings (or Date). Null when birthday missing/invalid.
 */
export function ageInYears(birthday, endDate = new Date()) {
  if (birthday == null || birthday === "") {
    return null;
  }
  const born = new Date(birthday);
  if (Number.isNaN(born.getTime())) {
    return null;
  }
  const end =
    endDate instanceof Date
      ? endDate
      : endDate
        ? new Date(endDate)
        : new Date();
  if (Number.isNaN(end.getTime())) {
    return null;
  }
  let age = end.getFullYear() - born.getFullYear();
  const monthDiff = end.getMonth() - born.getMonth();
  if (
    monthDiff < 0 ||
    (monthDiff === 0 && end.getDate() < born.getDate())
  ) {
    age -= 1;
  }
  return age >= 0 ? age : null;
}

/**
 * Person header facts. Empty fields omitted — no N/A.
 */
export function buildPersonFacts(person, now = new Date()) {
  if (person == null) {
    return [];
  }
  const facts = [];
  const bornLabel = formatDisplayDate(person.birthday);
  if (bornLabel != null) {
    const end = person.deathday || now;
    const age = ageInYears(person.birthday, end);
    facts.push({
      label: "Born",
      value: age != null ? `${bornLabel} (${age})` : bornLabel,
    });
  }
  const diedLabel = formatDisplayDate(person.deathday);
  if (diedLabel != null) {
    facts.push({ label: "Died", value: diedLabel });
  }
  if (person.placeOfBirth) {
    facts.push({ label: "Birthplace", value: person.placeOfBirth });
  }
  return facts;
}

/**
 * Role labels for a filmography row.
 */
export function formatCreditRoleLabels(roles) {
  if (!Array.isArray(roles) || roles.length === 0) {
    return [];
  }
  return roles
    .map((role) => {
      const base = role.label || (role.kind === "crew" ? role.department : null);
      if (base == null || base === "") {
        return null;
      }
      if (role.episodeCount != null && role.episodeCount > 0) {
        const unit = role.episodeCount === 1 ? "episode" : "episodes";
        return `${base} (${role.episodeCount} ${unit})`;
      }
      return base;
    })
    .filter(Boolean);
}
