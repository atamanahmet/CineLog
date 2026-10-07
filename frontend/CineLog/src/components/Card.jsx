import { Link } from "react-router";
import RatingCircle from "./RatingCircle";
import missing from "../assets/missing.png";
import { mediaReleaseDate, mediaReleaseYear } from "../utils/media";
import { getTmdbImageUrl } from "../lib/tmdbImage";
import { hasRating } from "../lib/hasRating";
import useAdultPolicy from "../hooks/useAdultPolicy";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

/** Shared root size for Card and CardSkeleton. */
export const CARD_SIZE_CLASS =
  "flex h-full w-full min-w-0 flex-col overflow-hidden sm:w-[215px]";

/** Shared poster frame for Card and CardSkeleton. */
export const CARD_POSTER_CLASS =
  "relative aspect-[2/3] w-full shrink-0 overflow-hidden";

/** Shared info footer for Card and CardSkeleton. */
export const CARD_FOOTER_CLASS =
  "flex min-h-0 flex-1 flex-row items-center justify-between gap-2 border-t border-border bg-card/70 px-3 py-3";

/**
 * Poster card loading placeholder. Matches Card root size.
 */
export function CardSkeleton({ className }) {
  return (
    <Skeleton
      shell
      className={cn("rounded-lg", CARD_SIZE_CLASS, className)}
    >
      <div className={CARD_POSTER_CLASS} />
      <div className={CARD_FOOTER_CLASS}>
        <div className="min-w-0 flex-1 space-y-2">
          <Skeleton className="h-4 w-3/4 rounded-sm" />
          <Skeleton className="h-3 w-1/2 rounded-sm" />
        </div>
      </div>
    </Skeleton>
  );
}

/**
 * Poster card. Optional stretched Link covers the card; actions stay above.
 */
function Card({ item, to, linkState }) {
  const { blurAdult } = useAdultPolicy();
  const blurred = Boolean(item?.adult) && blurAdult;
  const imageUrl = !item.posterPath
    ? missing
    : getTmdbImageUrl(item.posterPath, "poster", "w500");

  const dateStr = mediaReleaseDate(item);
  const releaseYear = mediaReleaseYear(item);
  const isValid = dateStr && releaseYear !== "Unknown";
  const displayTitle =
    item.originalLanguage == "en" ? item.originalTitle : item.title;

  if (!item) {
    return <CardSkeleton />;
  }

  const titleClassName =
    "title line-clamp-2 text-sm font-medium text-foreground";

  const titleNode = to ? (
    <Tooltip>
      <TooltipTrigger asChild>
        <Link
          to={to}
          state={linkState}
          className={cn(
            titleClassName,
            "after:absolute after:inset-0 after:z-[1] after:content-['']",
            "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring",
          )}
        >
          {displayTitle}
        </Link>
      </TooltipTrigger>
      <TooltipContent side="right" align="start">
        <span className="font-bold">Title: </span>
        {item.originalTitle}
        <br />
        <span className="font-bold"> Release Date: </span>
        {dateStr}
        <br />
        <span className="font-bold"> Original Language: </span>
        {item.originalLanguage}
      </TooltipContent>
    </Tooltip>
  ) : (
    <p className={titleClassName}>{displayTitle}</p>
  );

  return (
    <div
      className={cn(
        "group relative isolate rounded-lg border border-border bg-card shadow-lg shadow-foreground/15 transition duration-300 hover:-translate-y-1 hover:border-primary/80 hover:shadow-xl hover:shadow-primary/35",
        CARD_SIZE_CLASS,
      )}
    >
      <div className={CARD_POSTER_CLASS}>
        <img
          src={imageUrl}
          alt=""
          className={cn(
            "absolute inset-0 h-full w-full max-w-none object-cover",
            blurred && "scale-110 blur-2xl",
          )}
        />

        {blurred && (
          <div className="pointer-events-none absolute inset-0 z-[5] flex flex-col items-center justify-center gap-1 bg-black/30 text-white">
            <span className="rounded-md bg-destructive px-2 py-0.5 text-sm font-bold">
              18+
            </span>
            <span className="text-xs font-medium">Adult content</span>
          </div>
        )}
      </div>

      <div className={CARD_FOOTER_CLASS}>
        <div className="min-w-0 flex-1">
          {titleNode}
          <p className="text-xs text-foreground/70">
            {isValid
              ? new Date(dateStr).toISOString().slice(0, 10)
              : "Unknown"}
          </p>
        </div>

        {hasRating(item.voteAverage) && (
          <div
            className="shrink-0 rounded-full shadow-md shadow-primary/30"
            aria-label="rating"
          >
            <RatingCircle percentage={item.voteAverage.toFixed(1)} />
          </div>
        )}
      </div>
    </div>
  );
}

export default Card;
