import { Check } from "lucide-react";
import missing from "../assets/missing.png";
import { getTmdbImageUrl, getTmdbSrcSet } from "../lib/tmdbImage";
import { mediaReleaseYear } from "../utils/media";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/**
 * Shared poster frame size for SeedPosterCard and SeedPosterSkeleton.
 * Matches main Card aspect ratio (215×400).
 */
export const SEED_POSTER_FRAME_CLASS =
  "relative aspect-[215/400] w-full overflow-hidden rounded-md bg-card";

/**
 * Skeleton placeholder matching SeedPosterCard layout.
 */
export function SeedPosterSkeleton({ className }) {
  return (
    <div className={cn("flex flex-col gap-2", className)} aria-hidden="true">
      <Skeleton shell className={SEED_POSTER_FRAME_CLASS} />
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-3 w-1/2" />
    </div>
  );
}

/**
 * Calendar year number from a media item, or null.
 */
function itemYear(item) {
  const year = mediaReleaseYear(item);
  return year === "Unknown" ? null : year;
}

/**
 * Small selectable poster card for the seed search modal grid.
 */
export default function SeedPosterCard({
  item,
  selected,
  disabled,
  onToggle,
}) {
  const year = itemYear(item);
  const typeLabel = item.mediaType === "TV" ? "TV" : "Movie";
  const hasPoster = Boolean(item.posterPath);

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onToggle(item)}
      className={cn(
        "flex flex-col gap-2 text-left transition-opacity",
        disabled && "cursor-not-allowed opacity-50",
      )}
    >
      <div
        className={cn(
          SEED_POSTER_FRAME_CLASS,
          selected && "ring-2 ring-primary ring-offset-2 ring-offset-background",
        )}
      >
        {hasPoster ? (
          <img
            src={getTmdbImageUrl(item.posterPath, "poster", "w185")}
            srcSet={getTmdbSrcSet(item.posterPath, "poster")}
            sizes="(max-width: 640px) 33vw, (max-width: 768px) 25vw, 20vw"
            alt=""
            className="absolute inset-0 h-full w-full object-cover"
          />
        ) : (
          <img
            src={missing}
            alt=""
            className="absolute inset-0 h-full w-full object-cover"
          />
        )}
        {selected && (
          <span className="absolute top-1.5 right-1.5 flex size-5 items-center justify-center rounded-full bg-primary text-primary-foreground">
            <Check className="size-3" aria-hidden="true" />
          </span>
        )}
      </div>
      <p className="line-clamp-2 text-sm font-medium text-foreground">
        {item.title}
      </p>
      <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <span>{year != null ? year : "Unknown"}</span>
        <span className="rounded border border-border px-1.5 py-0.5 text-[10px] font-medium uppercase text-muted-foreground">
          {typeLabel}
        </span>
      </p>
    </button>
  );
}
