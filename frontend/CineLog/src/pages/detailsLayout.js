/** Shared page shell for the details view and its skeleton. */
export const DETAILS_PAGE_CLASS =
  "min-h-screen bg-background pb-20 text-foreground";

/** Content column below the hero and inside the hero. */
export const DETAILS_CONTAINER_CLASS =
  "mx-auto max-w-7xl px-4 md:px-8";

/** Vertical padding on the hero section. No margin on children. */
export const DETAILS_HERO_SECTION_CLASS = "py-6 md:py-8";

/** Padding inside the glass card. */
export const DETAILS_GLASS_INNER_CLASS = "p-6 md:p-8";

/** Shared poster and text row. */
export const DETAILS_ROW_CLASS =
  "flex flex-col gap-8 md:flex-row md:items-center";

/** Shared poster frame. Fixed 2/3 ratio. self-start stops flex stretch. */
export const DETAILS_POSTER_CLASS =
  "relative mx-auto aspect-[2/3] w-64 shrink-0 self-start overflow-hidden rounded-lg bg-background shadow-2xl md:mx-0 md:w-80 md:self-center";

/** Shared text column beside the poster. */
export const DETAILS_INFO_CLASS = "min-w-0 flex-1";

/** Shared cast grid. */
export const DETAILS_CAST_GRID_CLASS =
  "grid grid-cols-2 gap-4 md:grid-cols-4 lg:grid-cols-8";

/** Glass panel behind the hero content. */
export const DETAILS_GLASS_CLASS =
  "rounded-2xl border border-border/30 bg-background/30 backdrop-blur-md";

/** Gap below the hero before cast. */
export const DETAILS_CAST_SECTION_CLASS = "mt-12";

/**
 * Backdrop fade gradient. Clear through 60%, via background/40 at 80%,
 * solid background at 100%. One definition for light, dark, and skeleton.
 */
export const DETAILS_BACKDROP_GRADIENT_CLASS =
  "absolute inset-0 bg-gradient-to-b from-transparent from-60% via-background/40 via-80% to-background";
