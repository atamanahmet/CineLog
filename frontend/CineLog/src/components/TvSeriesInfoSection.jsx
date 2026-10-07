import { Link } from "react-router";
import missing from "../assets/missing.png";
import { getTmdbImageUrl } from "../lib/tmdbImage";
import { yearFromDate } from "../lib/displayDate";
import {
  buildTvSeriesFacts,
  formatNextEpisodeLine,
} from "../lib/tvSeriesFacts";
import {
  DETAILS_GLASS_CLASS,
  DETAILS_GLASS_INNER_CLASS,
} from "../pages/detailsLayout";
import { cn } from "@/lib/utils";

/**
 * TV-only series info panel and season strip.
 */
export default function TvSeriesInfoSection({ detail }) {
  const facts = buildTvSeriesFacts(detail);
  const nextLine = formatNextEpisodeLine(detail?.nextEpisodeToAir);
  const seasons = Array.isArray(detail?.seasons) ? detail.seasons : [];

  if (facts.length === 0 && !nextLine && seasons.length === 0) {
    return null;
  }

  return (
    <section className="mt-8 min-w-0">
      {(facts.length > 0 || nextLine) && (
        <div className={cn(DETAILS_GLASS_CLASS, DETAILS_GLASS_INNER_CLASS)}>
          <h2 className="mb-4 text-2xl font-semibold">Series info</h2>
          {facts.length > 0 && (
            <dl className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-3">
              {facts.map((fact) => (
                <div key={fact.label} className="min-w-0">
                  <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {fact.label}
                  </dt>
                  <dd className="text-sm text-foreground">
                    {fact.creators ? (
                      <CreatorList creators={fact.creators} />
                    ) : (
                      fact.value
                    )}
                  </dd>
                </div>
              ))}
            </dl>
          )}
          {nextLine && (
            <p className="mt-4 text-sm font-medium text-foreground">{nextLine}</p>
          )}
        </div>
      )}

      {seasons.length > 0 && (
        <div className="mt-6 min-w-0">
          <h3 className="mb-3 text-lg font-semibold">Seasons</h3>
          <div className="-mx-4 min-w-0 overflow-x-auto px-4 md:mx-0 md:px-0">
            <div className="flex w-max min-w-0 gap-3 pb-2">
              {seasons.map((season) => (
                <SeasonCard key={season.seasonNumber} season={season} />
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function CreatorList({ creators }) {
  return (
    <span className="flex flex-wrap gap-x-1">
      {creators.map((person, index) => (
        <span key={person.id}>
          {index > 0 && ", "}
          <Link
            to={`/person/${person.id}`}
            className="text-primary underline-offset-4 hover:underline"
          >
            {person.name}
          </Link>
        </span>
      ))}
    </span>
  );
}

function SeasonCard({ season }) {
  const posterPath = season.posterPath;
  const imageUrl =
    posterPath != null && posterPath !== ""
      ? getTmdbImageUrl(posterPath, "poster", "w185")
      : null;
  const year = yearFromDate(season.airDate);
  const episodeLabel =
    season.episodeCount != null
      ? `${season.episodeCount} episodes`
      : null;

  return (
    <div className="w-28 shrink-0 rounded-lg border border-border/30 bg-card text-left">
      <div className="relative aspect-[2/3] w-full overflow-hidden rounded-t-lg bg-muted">
        <img
          src={imageUrl ?? missing}
          alt=""
          className="size-full object-cover"
        />
      </div>
      <div className="space-y-0.5 p-2">
        <p className="line-clamp-2 text-xs font-medium text-foreground">
          {season.name ?? `Season ${season.seasonNumber}`}
        </p>
        {episodeLabel && (
          <p className="text-[11px] text-muted-foreground">{episodeLabel}</p>
        )}
        {year != null && (
          <p className="text-[11px] text-muted-foreground">{year}</p>
        )}
      </div>
    </div>
  );
}
