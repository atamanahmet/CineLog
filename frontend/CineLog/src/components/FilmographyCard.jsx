import { Link } from "react-router";
import missing from "../assets/missing.png";
import { getTmdbImageUrl } from "../lib/tmdbImage";
import { yearFromDate } from "../lib/displayDate";
import { formatCreditRoleLabels } from "../lib/personFacts";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import {
  ACTOR_CREDIT_POSTER_CLASS,
  ACTOR_CREDIT_ROW_CLASS,
} from "../pages/actorLayout";
import { Star } from "lucide-react";

/**
 * Filmography row loading placeholder. Matches FilmographyCard size.
 */
export function FilmographyCardSkeleton({ className }) {
  return (
    <div
      className={cn(ACTOR_CREDIT_ROW_CLASS, "pointer-events-none", className)}
      aria-hidden="true"
    >
      <Skeleton shell className={cn(ACTOR_CREDIT_POSTER_CLASS, "shadow-none")} />
      <div className="min-w-0 flex-1 space-y-2">
        <Skeleton className="h-4 w-2/3 max-w-xs" />
        <Skeleton className="h-3 w-1/2 max-w-[12rem]" />
      </div>
      <Skeleton className="h-4 w-10 shrink-0" />
    </div>
  );
}

/**
 * Compact filmography row. Whole card links to Details.
 */
function FilmographyCard({ item }) {
  const imageUrl = !item.posterPath
    ? missing
    : getTmdbImageUrl(item.posterPath, "poster", "w185");
  const year = yearFromDate(item.date);
  const mediaType = item.mediaType === "tv" ? "tv" : "movie";
  const roleLabels = formatCreditRoleLabels(item.roles);
  const rating =
    typeof item.voteAverage === "number" && item.voteAverage > 0
      ? item.voteAverage.toFixed(1)
      : null;

  return (
    <Link
      to={`/details/${mediaType}/${item.tmdbId}`}
      className={ACTOR_CREDIT_ROW_CLASS}
    >
      <img
        src={imageUrl}
        alt=""
        className={ACTOR_CREDIT_POSTER_CLASS}
        onError={(e) => {
          e.currentTarget.src = missing;
        }}
      />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-foreground">
          {item.title}
        </p>
        {roleLabels.length > 0 && (
          <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
            {roleLabels.join(" · ")}
          </p>
        )}
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1 text-xs text-muted-foreground">
        {year != null && (
          <span className="tabular-nums text-foreground">{year}</span>
        )}
        {rating != null && (
          <span className="inline-flex items-center gap-0.5 tabular-nums">
            <Star className="size-3 text-accent" aria-hidden />
            {rating}
          </span>
        )}
      </div>
    </Link>
  );
}

export default FilmographyCard;
