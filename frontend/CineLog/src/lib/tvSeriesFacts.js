import { formatDisplayDate } from "./displayDate";

/**
 * Build labeled TV series facts. Omits entries with null or empty values.
 */
export function buildTvSeriesFacts(detail) {
  if (detail == null) {
    return [];
  }
  const facts = [];

  pushFact(facts, "Status", detail.status);

  if (detail.numberOfSeasons != null) {
    pushFact(
      facts,
      "Seasons",
      String(detail.numberOfSeasons),
    );
  }

  if (detail.numberOfEpisodes != null) {
    pushFact(
      facts,
      "Episodes",
      String(detail.numberOfEpisodes),
    );
  }

  if (detail.episodeRunTime != null) {
    pushFact(facts, "Runtime", `${detail.episodeRunTime} min`);
  }

  const firstAired =
    formatDisplayDate(detail.firstAirDate ?? detail.releaseDate);
  pushFact(facts, "First aired", firstAired);

  const lastAired = formatDisplayDate(detail.lastAirDate);
  pushFact(facts, "Last aired", lastAired);

  if (Array.isArray(detail.networks) && detail.networks.length > 0) {
    pushFact(facts, "Networks", detail.networks.join(", "));
  }

  if (Array.isArray(detail.createdBy) && detail.createdBy.length > 0) {
    facts.push({
      label: "Created by",
      creators: detail.createdBy.filter(
        (row) => row?.id != null && row?.name,
      ),
    });
  }

  return facts;
}

/**
 * Next episode line for TV details, or null when not scheduled.
 */
export function formatNextEpisodeLine(nextEpisode) {
  if (nextEpisode == null) {
    return null;
  }
  const { seasonNumber, episodeNumber, airDate } = nextEpisode;
  if (seasonNumber == null || episodeNumber == null) {
    return null;
  }
  const dateLabel = formatDisplayDate(airDate);
  if (dateLabel == null) {
    return `Next: S${seasonNumber}E${episodeNumber}`;
  }
  return `Next: S${seasonNumber}E${episodeNumber} on ${dateLabel}`;
}

function pushFact(facts, label, value) {
  if (value == null || value === "") {
    return;
  }
  facts.push({ label, value });
}
