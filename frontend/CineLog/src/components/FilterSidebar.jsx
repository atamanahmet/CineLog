import { useEffect, useState } from "react";
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  Filter,
  Globe,
  Hash,
  RotateCcw,
  SlidersHorizontal,
  Star,
} from "lucide-react";
import { useLocation } from "react-router";
import { useUserPrefsStore } from "../stores/userPrefsStore";
import {
  clampFilterValue,
  emptyFilters,
  filtersMatchPreset,
  showsRuntimeFilter,
  showsVoteCountFilter,
} from "../lib/sanitizeFilters";
import { filterScopeKey, YEAR_FILTER_SCOPES } from "../lib/filterScope";
import { getPresetFilters } from "../lib/pagePresets";
import {
  countActiveFilters,
  formatActiveFilterBadge,
} from "../lib/countActiveFilters";
import {
  cycleFilterValue,
  emptyTriState,
} from "../lib/triStateFilter";
import { genreChipsForView } from "../lib/genreLabels";
import useGenreOptions from "../hooks/useGenreOptions";
import usePageFilters from "../hooks/usePageFilters";
import useAdultPolicy from "../hooks/useAdultPolicy";
import useAgeGate from "../hooks/useAgeGate";
import AgeGateDialog from "./AgeGateDialog";
import TriStateChips from "./TriStateChips";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

const REC_GENRE_FILTER_SCOPES = new Set(["Recommendation", "Find similar"]);
const TMDB_GENRE_FILTER_SCOPES = new Set(["discover", "new", "top", "upcoming"]);
const CLIENT_GENRE_FILTER_SCOPES = new Set([
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
  "search",
]);
const GENRE_FILTER_SCOPES = new Set([
  ...REC_GENRE_FILTER_SCOPES,
  ...TMDB_GENRE_FILTER_SCOPES,
  ...CLIENT_GENRE_FILTER_SCOPES,
]);

/**
 * Language tri-state chips. Rec/Similar omitted: RecommendationItemDTO
 * and MediaDisplay lack originalLanguage (see L1 report).
 * Discover/New/Top use chips + client filter (L2); TMDB hint when one include.
 */
const CLIENT_LANGUAGE_CHIP_SCOPES = new Set([
  "discover",
  "new",
  "top",
  "upcoming",
  "Watchlist",
  "Watchedlist",
  "Loved",
  "Not interested",
  "search",
]);

const LANGUAGES = [
  { code: "en", name: "English" },
  { code: "tr", name: "Turkish" },
  { code: "es", name: "Spanish" },
  { code: "fr", name: "French" },
  { code: "de", name: "German" },
  { code: "it", name: "Italian" },
  { code: "ja", name: "Japanese" },
  { code: "ko", name: "Korean" },
  { code: "zh", name: "Mandarin" },
];

const LANGUAGE_CHIPS = LANGUAGES.map((lang) => ({
  value: lang.code,
  label: lang.name,
}));

/**
 * Map legacy { id, name } genre chips to TriStateChips { value, label }.
 */
function toValueLabelChips(chips) {
  if (!Array.isArray(chips)) {
    return [];
  }
  return chips.map((chip) =>
    chip.value != null
      ? { value: chip.value, label: chip.label ?? chip.name }
      : { value: chip.id, label: chip.name },
  );
}

function RangePair({ min, max, step = 1, low, high, onLow, onHigh }) {
  return (
    <div className="w-full min-w-0 space-y-2">
      <div className="flex justify-between text-xs text-muted-foreground">
        <span>{low}</span>
        <span>{high}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={low}
        onChange={(e) => onLow(e.target.value)}
        className="w-full min-w-0 accent-primary"
      />
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={high}
        onChange={(e) => onHigh(e.target.value)}
        className="w-full min-w-0 accent-primary"
      />
    </div>
  );
}

/**
 * Single-value range. Local draft while dragging, commit on release.
 */
function CommitRange({ min, max, step = 1, value, onCommit, label }) {
  const [draft, setDraft] = useState(value);

  useEffect(() => {
    setDraft(value);
  }, [value]);

  const commit = () => {
    const next = clampFilterValue(draft, min, max);
    if (next !== value) {
      onCommit(next);
    } else if (next !== draft) {
      setDraft(next);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-2">
      <div className="flex min-w-0 justify-between gap-2 text-xs text-muted-foreground">
        <span className="min-w-0 break-words">{label}</span>
        <span className="shrink-0">{draft}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={draft}
        onChange={(e) => setDraft(Number(e.target.value))}
        onPointerUp={commit}
        onKeyUp={commit}
        onBlur={commit}
        className="w-full min-w-0 accent-primary"
        aria-label={label}
      />
    </div>
  );
}

/**
 * Filter form fields, shared by the desktop panel and the mobile sheet.
 */
function FilterContent({ bounds, activeKeys, genreMediaType, genreChips }) {
  const location = useLocation();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const profileTab = useUserPrefsStore((s) => s.profileTab);
  const mediaType = genreMediaType ?? storeMediaType;
  const scopeKey = filterScopeKey(location.pathname, profileTab);
  const { genres, isLoading: genresLoading } = useGenreOptions(mediaType);
  const { filters, setFilters } = usePageFilters(scopeKey, mediaType, scopeKey);
  const showGenreFilter = GENRE_FILTER_SCOPES.has(scopeKey);
  const showTmdbGenreChips = TMDB_GENRE_FILTER_SCOPES.has(scopeKey);
  const showVoteCount =
    showsVoteCountFilter(location.pathname) &&
    (activeKeys == null || activeKeys.includes("voteCount"));
  const showRuntime =
    showsRuntimeFilter(location.pathname) &&
    (activeKeys == null || activeKeys.includes("runtime"));
  const showYear =
    YEAR_FILTER_SCOPES.has(scopeKey) &&
    (activeKeys == null || activeKeys.includes("yearRange"));
  const showRating = activeKeys == null || activeKeys.includes("rating");
  const showLanguageChips = CLIENT_LANGUAGE_CHIP_SCOPES.has(scopeKey);
  const showLanguageCheckboxes =
    !showLanguageChips &&
    (activeKeys == null || activeKeys.includes("languages"));
  const showAdult = activeKeys == null || activeKeys.includes("adult");
  const presetFilters = getPresetFilters(scopeKey, mediaType);
  const voteBaseline = presetFilters.voteCount ?? bounds.minVotesDefault;

  const patch = (partial) => setFilters({ ...filters, ...partial }, bounds);

  const cycleGenreChip = (id) => {
    const current = filters.genreFilter ?? emptyTriState();
    patch({ genreFilter: cycleFilterValue(current, id) });
  };

  const cycleLanguageChip = (code) => {
    const current = filters.languageFilter ?? emptyTriState();
    patch({ languageFilter: cycleFilterValue(current, code) });
  };

  const toggleLanguage = (code) => {
    const languages = filters.languages.includes(code)
      ? filters.languages.filter((l) => l !== code)
      : [...filters.languages, code];
    patch({ languages });
  };

  const genreChipList = showTmdbGenreChips
    ? toValueLabelChips(genres)
    : toValueLabelChips(genreChips ?? genreChipsForView(mediaType));

  const setYearOrRating = (field, index, raw) => {
    const range = bounds[field];
    const current = filters[field] ?? [range.min, range.max];
    const next = [...current];
    next[index] = clampFilterValue(raw, range.min, range.max);
    patch({ [field]: next });
  };

  const setRuntimeBound = (index, raw) => {
    const low =
      filters.minRuntime ?? bounds.runtime.min;
    const high =
      filters.maxRuntime ?? bounds.runtime.max;
    const next = [low, high];
    next[index] = clampFilterValue(raw, bounds.runtime.min, bounds.runtime.max);
    let minRuntime = next[0];
    let maxRuntime = next[1];
    if (minRuntime > maxRuntime) {
      const swap = minRuntime;
      minRuntime = maxRuntime;
      maxRuntime = swap;
    }
    if (
      minRuntime === bounds.runtime.min &&
      maxRuntime === bounds.runtime.max
    ) {
      patch({ minRuntime: null, maxRuntime: null });
      return;
    }
    patch({ minRuntime, maxRuntime });
  };

  const commitVoteCount = (value) => {
    if (value === voteBaseline) {
      patch({ voteCount: null });
      return;
    }
    patch({ voteCount: value });
  };

  const yearLow = filters.yearRange?.[0] ?? bounds.yearRange.min;
  const yearHigh = filters.yearRange?.[1] ?? bounds.yearRange.max;
  const ratingLow = filters.rating?.[0] ?? bounds.rating.min;
  const ratingHigh = filters.rating?.[1] ?? bounds.rating.max;
  const runtimeLow = filters.minRuntime ?? bounds.runtime.min;
  const runtimeHigh = filters.maxRuntime ?? bounds.runtime.max;
  const voteDisplay = filters.voteCount ?? voteBaseline;

  return (
    <div className="flex w-full min-w-0 flex-col gap-4">
      {showGenreFilter && showTmdbGenreChips && genresLoading ? (
        <section className="min-w-0">
          <h3 className="px-1 pb-2 text-sm font-medium text-foreground/80">
            Genres
          </h3>
          <div aria-busy="true" className="space-y-2 px-1 py-1">
            <span className="sr-only">Loading genres</span>
            <Skeleton className="h-8 w-24" />
            <Skeleton className="h-8 w-28" />
            <Skeleton className="h-8 w-20" />
          </div>
        </section>
      ) : null}

      {showGenreFilter && !(showTmdbGenreChips && genresLoading) ? (
        <TriStateChips
          title="Genres"
          filter={filters.genreFilter ?? emptyTriState()}
          onCycle={cycleGenreChip}
          chips={genreChipList}
        />
      ) : null}

      {showLanguageChips ? (
        <TriStateChips
          title={
            <span className="flex items-center gap-2">
              <Globe className="size-4 text-primary" />
              Languages
            </span>
          }
          filter={filters.languageFilter ?? emptyTriState()}
          onCycle={cycleLanguageChip}
          chips={LANGUAGE_CHIPS}
        />
      ) : null}

      {showYear && (
      <section>
        <h3 className="flex items-center gap-2 px-1 pb-2 text-sm font-medium text-foreground/80">
          <Calendar className="size-4 text-primary" />
          Year
        </h3>
        <div className="px-1">
          <RangePair
            min={bounds.yearRange.min}
            max={bounds.yearRange.max}
            low={yearLow}
            high={yearHigh}
            onLow={(v) => setYearOrRating("yearRange", 0, v)}
            onHigh={(v) => setYearOrRating("yearRange", 1, v)}
          />
        </div>
      </section>
      )}

      {showRating && (
      <section>
        <h3 className="flex items-center gap-2 px-1 pb-2 text-sm font-medium text-foreground/80">
          <Star className="size-4 text-primary" />
          Rating
        </h3>
        <div className="px-1">
          <RangePair
            min={bounds.rating.min}
            max={bounds.rating.max}
            step={0.5}
            low={ratingLow}
            high={ratingHigh}
            onLow={(v) => setYearOrRating("rating", 0, v)}
            onHigh={(v) => setYearOrRating("rating", 1, v)}
          />
        </div>
      </section>
      )}

      {showVoteCount && (
        <section>
          <h3 className="flex items-center gap-2 px-1 pb-2 text-sm font-medium text-foreground/80">
            <Hash className="size-4 text-primary" />
            Minimum votes
          </h3>
          <div className="px-1">
            <CommitRange
              min={bounds.voteCount.min}
              max={bounds.voteCount.max}
              step={50}
              value={voteDisplay}
              label="Minimum votes"
              onCommit={commitVoteCount}
            />
          </div>
        </section>
      )}

      {showRuntime && (
        <section>
          <h3 className="flex items-center gap-2 px-1 pb-2 text-sm font-medium text-foreground/80">
            <Clock className="size-4 text-primary" />
            Runtime (min)
          </h3>
          <div className="px-1">
            <RangePair
              min={bounds.runtime.min}
              max={bounds.runtime.max}
              step={5}
              low={runtimeLow}
              high={runtimeHigh}
              onLow={(v) => setRuntimeBound(0, v)}
              onHigh={(v) => setRuntimeBound(1, v)}
            />
          </div>
        </section>
      )}

      {showLanguageCheckboxes && (
      <section>
        <h3 className="flex items-center gap-2 px-1 pb-2 text-sm font-medium text-foreground/80">
          <Globe className="size-4 text-primary" />
          Languages
        </h3>
        <div className="grid min-w-0 grid-cols-1 gap-1 px-1">
          {LANGUAGES.map((lang) => (
            <label
              key={lang.code}
              className="flex min-w-0 cursor-pointer items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
            >
              <input
                type="checkbox"
                checked={filters.languages.includes(lang.code)}
                onChange={() => toggleLanguage(lang.code)}
                className="shrink-0 rounded border-primary bg-secondary text-primary"
              />
              <span className="min-w-0 break-words">{lang.name}</span>
            </label>
          ))}
        </div>
      </section>
      )}

      {showAdult && <AdultFilterToggle bounds={bounds} />}
    </div>
  );
}

/**
 * Adult results toggle. Rendered only when the site allows adult content; turning it on asks for age.
 */
function AdultFilterToggle({ bounds }) {
  const location = useLocation();
  const { enabled, includeAdult } = useAdultPolicy();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const profileTab = useUserPrefsStore((s) => s.profileTab);
  const setIncludeAdult = useUserPrefsStore((s) => s.setIncludeAdult);
  const scopeKey = filterScopeKey(location.pathname, profileTab);
  const { filters, setFilters } = usePageFilters(
    scopeKey,
    storeMediaType,
    scopeKey,
  );
  const { requestConfirmation, dialogProps } = useAgeGate();

  if (!enabled) {
    return null;
  }

  const applyAdult = (adult) => {
    setIncludeAdult(adult);
    setFilters({ ...filters, adult }, bounds);
  };

  const handleChange = () => {
    if (includeAdult) {
      applyAdult(false);
      return;
    }
    requestConfirmation(() => applyAdult(true));
  };

  return (
    <section>
      <h3 className="px-1 pb-2 text-sm font-medium text-foreground/80">
        Adult content
      </h3>
      <label className="flex cursor-pointer items-center gap-2 px-1 text-sm text-muted-foreground">
        <input
          type="checkbox"
          role="switch"
          checked={includeAdult}
          onChange={handleChange}
          className="rounded border-primary bg-secondary text-primary"
        />
        Include adult results
      </label>
      <AgeGateDialog {...dialogProps} />
    </section>
  );
}

/**
 * Collapsed rail. Fixed overlay docked beside the content column, so
 * toggling never shifts page layout.
 */
function CollapsedFilterRail({ onExpand, bounds, genreMediaType }) {
  const location = useLocation();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const profileTab = useUserPrefsStore((s) => s.profileTab);
  const mediaType = genreMediaType ?? storeMediaType;
  const scopeKey = filterScopeKey(location.pathname, profileTab);
  const { filters } = usePageFilters(scopeKey, mediaType, scopeKey);
  const defaults = emptyFilters(bounds);
  const scope = {
    languages: CLIENT_LANGUAGE_CHIP_SCOPES.has(scopeKey),
    yearRange: YEAR_FILTER_SCOPES.has(scopeKey),
  };
  const activeCount = countActiveFilters(filters, defaults, scope);
  const badgeLabel = formatActiveFilterBadge(activeCount);
  const ariaLabel =
    activeCount > 0 ? `Filters, ${activeCount} active` : "Show filters";

  return (
    <aside className={`fixed top-[calc(var(--header-height)+1rem)] left-[max(1rem,calc(50%-640px-4rem))] z-40 hidden w-12 rounded-xl border border-border bg-card text-foreground shadow-lg lg:flex`}>
      <button
        type="button"
        onClick={onExpand}
        aria-expanded="false"
        aria-label={ariaLabel}
        className="group flex w-full cursor-pointer flex-col items-center gap-3 py-3 transition-colors hover:bg-accent hover:text-accent-foreground"
      >
        <span className="relative flex size-8 items-center justify-center rounded-md border border-border bg-secondary text-muted-foreground transition-colors group-hover:border-primary group-hover:text-accent-foreground">
          <Filter className="size-4" />
          {badgeLabel ? (
            <Badge
              aria-hidden="true"
              className="absolute -right-1 -top-1 min-w-4 justify-center px-1 py-0"
            >
              {badgeLabel}
            </Badge>
          ) : null}
        </span>
        <span className="text-xs font-medium uppercase tracking-widest text-muted-foreground transition-colors [writing-mode:vertical-rl] group-hover:text-accent-foreground">
          Filters
        </span>
        <ChevronRight className="mt-auto size-4 text-primary transition-colors group-hover:text-accent-foreground" />
      </button>
    </aside>
  );
}

/**
 * Reset filters to the current page preset. Disabled when already at preset.
 */
function ResetFiltersButton({ bounds, className, genreMediaType }) {
  const location = useLocation();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const profileTab = useUserPrefsStore((s) => s.profileTab);
  const mediaType = genreMediaType ?? storeMediaType;
  const scopeKey = filterScopeKey(location.pathname, profileTab);
  const { filters, resetFilters } = usePageFilters(scopeKey, mediaType, scopeKey);
  const atPreset = filtersMatchPreset(filters, bounds);

  return (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      onClick={() => resetFilters()}
      disabled={atPreset}
      className={className}
      aria-label="Reset filters"
    >
      <RotateCcw className="size-3.5" />
      Reset
    </Button>
  );
}

/**
 * Floating desktop filter panel. Fixed overlay docked beside the content
 * column, outside document flow, so it overlaps content on narrow screens.
 */
function ExpandedFilterPanel({
  bounds,
  activeKeys,
  genreMediaType,
  genreChips,
  onCollapse,
}) {
  return (
    <aside className={`fixed top-[calc(var(--header-height)+1rem)] left-[max(1rem,calc(50%-640px-17rem))] z-40 hidden max-h-[calc(100vh-var(--header-height)-2rem)] w-64 flex-col overflow-hidden rounded-xl border border-border bg-card text-foreground shadow-lg lg:flex`}>
      <div className="flex shrink-0 items-center justify-between gap-1 border-b border-border p-2 px-3">
        <span className="flex items-center gap-2 text-sm font-semibold">
          <Filter className="size-4 text-primary" />
          Filters
        </span>
        <div className="flex items-center gap-1">
          <ResetFiltersButton
            bounds={bounds}
            genreMediaType={genreMediaType}
            className="h-7 gap-1 px-2 text-xs text-muted-foreground hover:bg-accent hover:text-accent-foreground"
          />
          <Button
            variant="ghost"
            size="icon"
            onClick={onCollapse}
            aria-expanded="true"
            aria-label="Hide filters"
            className="h-7 w-7 text-foreground hover:bg-accent hover:text-accent-foreground"
          >
            <ChevronLeft className="size-4" />
          </Button>
        </div>
      </div>
      <div className="min-h-0 min-w-0 flex-1 overflow-y-auto overscroll-contain px-3 py-3">
        <FilterContent
          bounds={bounds}
          activeKeys={activeKeys}
          genreMediaType={genreMediaType}
          genreChips={genreChips}
        />
      </div>
    </aside>
  );
}

/**
 * Mobile filter trigger and sheet, shown only below the lg breakpoint.
 * Desktop rail/panel above handle lg and up.
 */
function MobileFilterSheet({ bounds, activeKeys, genreMediaType, genreChips }) {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const storeMediaType = useUserPrefsStore((s) => s.mediaType);
  const profileTab = useUserPrefsStore((s) => s.profileTab);
  const mediaType = genreMediaType ?? storeMediaType;
  const scopeKey = filterScopeKey(location.pathname, profileTab);
  const { filters } = usePageFilters(scopeKey, mediaType, scopeKey);
  const defaults = emptyFilters(bounds);
  const scope = {
    languages: CLIENT_LANGUAGE_CHIP_SCOPES.has(scopeKey),
    yearRange: YEAR_FILTER_SCOPES.has(scopeKey),
  };
  const activeCount = countActiveFilters(filters, defaults, scope);
  const badgeLabel = formatActiveFilterBadge(activeCount);
  const ariaLabel =
    activeCount > 0 ? `Filters, ${activeCount} active` : "Filters";

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          aria-label={ariaLabel}
          className="gap-2 border-border text-foreground hover:bg-accent hover:text-accent-foreground lg:hidden"
        >
          <SlidersHorizontal className="size-4" />
          Filters
          {badgeLabel ? (
            <Badge aria-hidden="true">{badgeLabel}</Badge>
          ) : null}
        </Button>
      </SheetTrigger>
      <SheetContent
        side="left"
        className="w-80 overflow-y-auto bg-card text-foreground"
      >
        <SheetHeader>
          <div className="flex items-center justify-between gap-2 pr-8">
            <SheetTitle className="text-foreground">Filters</SheetTitle>
            <ResetFiltersButton
              bounds={bounds}
              genreMediaType={genreMediaType}
              className="h-7 gap-1 px-2 text-xs text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            />
          </div>
        </SheetHeader>
        <div className="px-1 py-2">
          <FilterContent
            bounds={bounds}
            activeKeys={activeKeys}
            genreMediaType={genreMediaType}
            genreChips={genreChips}
          />
        </div>
      </SheetContent>
    </Sheet>
  );
}

/**
 * Filter chrome for catalog pages. Desktop shows a floating collapsible
 * rail or panel, closed by default, open state persisted in user prefs.
 * Mobile shows a sheet trigger. Children keep full width.
 */
export default function FilterSidebar({
  children,
  bounds,
  activeKeys,
  genreMediaType,
  genreChips,
}) {
  const open = useUserPrefsStore((s) => s.filterPanelOpen);
  const setOpen = useUserPrefsStore((s) => s.setFilterPanelOpen);

  if (!bounds) {
    return children;
  }

  return (
    <>
      {open ? (
        <ExpandedFilterPanel
          bounds={bounds}
          activeKeys={activeKeys}
          genreMediaType={genreMediaType}
          genreChips={genreChips}
          onCollapse={() => setOpen(false)}
        />
      ) : (
        <CollapsedFilterRail
          onExpand={() => setOpen(true)}
          bounds={bounds}
          genreMediaType={genreMediaType}
        />
      )}

      <div className="page-container flex w-full min-w-0 items-center justify-start gap-2 py-2 lg:hidden">
        <MobileFilterSheet
          bounds={bounds}
          activeKeys={activeKeys}
          genreMediaType={genreMediaType}
          genreChips={genreChips}
        />
      </div>
      {children}
    </>
  );
}
