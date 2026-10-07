/** Equal segments; group never shrinks below label width. */
export const MEDIA_TYPE_GROUP_CLASS =
  "flex w-full min-w-fit shrink-0 rounded-md bg-secondary md:inline-flex md:w-auto";

/** Longest label is Cinema; keep equal flex share. */
export const MEDIA_TYPE_SEGMENT_CLASS =
  "min-w-[5.25rem] flex-1 whitespace-nowrap px-2 py-2 focus:outline-none md:px-4";

/**
 * Cinema, TV, and optional Mixed segment control.
 */
export default function MediaTypeToggle({
  mediaType,
  onChange,
  allowAll = false,
}) {
  const options = allowAll
    ? [
        { value: "movie", label: "Movie" },
        { value: "tv", label: "TV" },
        { value: "all", label: "Mixed" },
      ]
    : [
        { value: "movie", label: "Movie" },
        { value: "tv", label: "TV" },
      ];

  return (
    <div
      role="tablist"
      aria-label="Select media type"
      className={MEDIA_TYPE_GROUP_CLASS}
    >
      {options.map((option, index) => {
        const selected = mediaType === option.value;
        const isFirst = index === 0;
        const isLast = index === options.length - 1;
        const round = isFirst
          ? "rounded-l-md"
          : isLast
            ? "rounded-r-md"
            : "";
        return (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={selected}
            className={`${MEDIA_TYPE_SEGMENT_CLASS} ${round} ${
              selected
                ? "bg-primary text-primary-foreground"
                : "text-foreground hover:bg-accent hover:text-accent-foreground"
            }`}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
