import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import {
  DETAILS_BACKDROP_GRADIENT_CLASS,
  DETAILS_CONTAINER_CLASS,
  DETAILS_GLASS_CLASS,
  DETAILS_GLASS_INNER_CLASS,
  DETAILS_HERO_SECTION_CLASS,
} from "../pages/detailsLayout";

/**
 * Hero shell with a full height backdrop and a glass content card.
 */
export default function DetailsHero({
  backdropSrc,
  backdropSrcSet,
  backdropSizes,
  backdropAlt = "",
  backdropFallback,
  backdropImageClassName,
  children,
}) {
  const [imgSrc, setImgSrc] = useState(() =>
    backdropSrc ? backdropSrc : backdropFallback ?? null,
  );

  useEffect(() => {
    setImgSrc(backdropSrc ? backdropSrc : backdropFallback ?? null);
  }, [backdropSrc, backdropFallback]);

  const showImage = Boolean(imgSrc);

  return (
    <section
      className={cn("relative isolate", DETAILS_HERO_SECTION_CLASS)}
      data-slot="details-hero"
    >
      <div
        className="pointer-events-none absolute inset-0 -z-10 overflow-hidden bg-background"
        data-slot="backdrop"
        aria-hidden
      >
        {showImage && (
          <img
            src={imgSrc}
            srcSet={backdropSrcSet || undefined}
            sizes={backdropSrcSet ? backdropSizes : undefined}
            alt={backdropAlt}
            className={cn(
              "size-full object-cover object-top",
              backdropImageClassName,
            )}
            onError={() => {
              if (backdropFallback && imgSrc !== backdropFallback) {
                setImgSrc(backdropFallback);
              }
            }}
          />
        )}
        <div className={DETAILS_BACKDROP_GRADIENT_CLASS} />
      </div>
      <div className={DETAILS_CONTAINER_CLASS}>
        <div
          className={cn(
            DETAILS_GLASS_CLASS,
            DETAILS_GLASS_INNER_CLASS,
          )}
          data-slot="glass"
        >
          {children}
        </div>
      </div>
    </section>
  );
}
