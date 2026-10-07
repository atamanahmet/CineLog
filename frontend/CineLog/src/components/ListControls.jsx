import { useUserPrefsStore } from "../stores/userPrefsStore";
import { DEFAULT_SORT } from "../lib/sortOptions";
import SortSelect from "./SortSelect";

/**
 * Shared Movie/TV toggle and per-page sort dropdown for catalog routes.
 */
export default function ListControls({ page, options = [], disabled = false }) {
  const mediaType = useUserPrefsStore((s) => s.mediaType);
  const setMediaType = useUserPrefsStore((s) => s.setMediaType);
  const sortValue = useUserPrefsStore((s) => s.sortByPage?.[page] ?? null);
  const setSortForPage = useUserPrefsStore((s) => s.setSortForPage);

  const sortOptions = [
    { label: "Sort by", value: DEFAULT_SORT },
    ...options,
  ];

  return (
    <div className="page-container mt-6 mb-4 flex min-h-10 w-full min-w-0 flex-col gap-3 md:flex-row md:items-center md:justify-end">
      <div className="flex w-full min-w-0 items-center gap-3 md:w-auto md:justify-end md:flex-nowrap">
        <div
          role="tablist"
          aria-label="Select media type"
          className="inline-flex shrink-0 rounded-md bg-secondary"
        >
          <button
            type="button"
            role="tab"
            aria-selected={mediaType === "movie"}
            className={`rounded-l-md px-4 py-2 focus:outline-none cursor-pointer ${
              mediaType === "movie"
                ? "bg-primary text-primary-foreground"
                : "text-foreground hover:bg-accent hover:text-accent-foreground"
            }`}
            onClick={() => setMediaType("movie")}
          >
            Movie
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mediaType === "tv"}
            className={`rounded-r-md px-4 py-2 focus:outline-none cursor-pointer ${
              mediaType === "tv"
                ? "bg-primary text-primary-foreground"
                : "text-foreground hover:bg-accent hover:text-accent-foreground"
            }`}
            onClick={() => setMediaType("tv")}
          >
            TV
          </button>
        </div>

        <SortSelect
          ariaLabel="Sort by"
          className="min-w-0 flex-1 md:w-48 md:flex-none"
          value={sortValue ?? DEFAULT_SORT}
          disabled={disabled}
          options={sortOptions}
          onValueChange={(next) => {
            setSortForPage(page, next === DEFAULT_SORT ? null : next);
          }}
        />
      </div>
    </div>
  );
}
