import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import missing from "../assets/missing.png";
import FilmographyCard, {
  FilmographyCardSkeleton,
} from "../components/FilmographyCard";
import LoadingRegion from "../components/LoadingRegion";
import useActor from "../hooks/useActor";
import { getTmdbImageUrl } from "../lib/tmdbImage";
import { buildPersonFacts } from "../lib/personFacts";
import {
  departmentsForMedia,
  resolveDefaultDepartment,
  selectFilmographyGroups,
} from "../lib/personFilmography";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import {
  ACTOR_CONTAINER_CLASS,
  ACTOR_GLASS_CLASS,
  ACTOR_GLASS_INNER_CLASS,
  ACTOR_HEADER_ROW_CLASS,
  ACTOR_HERO_SECTION_CLASS,
  ACTOR_INFO_CLASS,
  ACTOR_PAGE_CLASS,
  ACTOR_PROFILE_CLASS,
  ACTOR_TOOLBAR_CLASS,
  ACTOR_YEAR_GROUP_CLASS,
} from "./actorLayout";

const MEDIA_TABS = [
  { value: "all", label: "All" },
  { value: "movie", label: "Movies" },
  { value: "tv", label: "TV" },
];

const SKELETON_ROW_COUNT = 8;

/**
 * Actor page loading layout. Shares constants with the real layout.
 */
function ActorPageSkeleton() {
  return (
    <LoadingRegion label="Loading person" className={ACTOR_PAGE_CLASS}>
      <section className={ACTOR_HERO_SECTION_CLASS}>
        <div className={ACTOR_CONTAINER_CLASS}>
          <div className={cn(ACTOR_GLASS_CLASS, ACTOR_GLASS_INNER_CLASS)}>
            <div className={ACTOR_HEADER_ROW_CLASS}>
              <Skeleton shell className={ACTOR_PROFILE_CLASS} />
              <div className={cn(ACTOR_INFO_CLASS, "space-y-4")}>
                <Skeleton className="h-10 w-2/3 max-w-md" />
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-4 w-56" />
                <Skeleton className="mt-4 h-4 w-full max-w-3xl" />
                <Skeleton className="h-4 w-full max-w-3xl" />
                <Skeleton className="h-4 w-3/4 max-w-2xl" />
              </div>
            </div>
          </div>
        </div>
      </section>
      <div className={ACTOR_CONTAINER_CLASS}>
        <div className={ACTOR_TOOLBAR_CLASS}>
          <Skeleton className="h-9 w-48" />
          <Skeleton className="h-9 w-40" />
        </div>
        <div className="mt-6 space-y-3">
          {Array.from({ length: SKELETON_ROW_COUNT }, (_, i) => (
            <FilmographyCardSkeleton key={i} />
          ))}
        </div>
      </div>
    </LoadingRegion>
  );
}

/**
 * Biography with ~5-line clamp and Read more / Show less.
 */
function BiographyBlock({ text }) {
  const [expanded, setExpanded] = useState(false);
  if (!text) {
    return null;
  }
  return (
    <div className="mt-6 max-w-3xl">
      <p
        className={cn(
          "whitespace-pre-line text-base leading-relaxed text-muted-foreground",
          !expanded && "line-clamp-5",
        )}
      >
        {text}
      </p>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="mt-2 px-0 text-primary hover:bg-transparent hover:text-accent"
        onClick={() => setExpanded((v) => !v)}
      >
        {expanded ? "Show less" : "Read more"}
      </Button>
    </div>
  );
}

function ActorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { person, credits, loading, error } = useActor(id);

  const [mediaTab, setMediaTab] = useState("all");
  const [department, setDepartment] = useState(null);
  const [showAppearances, setShowAppearances] = useState(false);
  const [deptTouched, setDeptTouched] = useState(false);

  const handleMediaTab = (value) => {
    setMediaTab(value);
    setDeptTouched(false);
  };

  const departments = useMemo(
    () => departmentsForMedia(credits, mediaTab),
    [credits, mediaTab],
  );

  useEffect(() => {
    if (departments.length === 0) {
      setDepartment(null);
      return;
    }
    if (deptTouched && department && departments.includes(department)) {
      return;
    }
    setDepartment(
      resolveDefaultDepartment(departments, person?.knownForDepartment),
    );
  }, [departments, person?.knownForDepartment, department, deptTouched]);

  const groups = useMemo(
    () =>
      selectFilmographyGroups(credits, {
        mediaTab,
        department,
        showAppearances,
      }),
    [credits, mediaTab, department, showAppearances],
  );

  if (error === "not-found") {
    return (
      <div className={cn(ACTOR_PAGE_CLASS, "pt-20 text-center")}>
        <p className="mb-6 text-lg">Person not found</p>
        <Link
          to="/"
          className="rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          Discover
        </Link>
      </div>
    );
  }

  if (error && !person) {
    return (
      <div className={cn(ACTOR_PAGE_CLASS, "pt-20 text-center")}>
        <p className="mb-6 text-lg">Could not load person</p>
        <button
          type="button"
          className="rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
          onClick={() => navigate(-1)}
        >
          Back
        </button>
      </div>
    );
  }

  if (loading || !person) {
    return <ActorPageSkeleton />;
  }

  const profileUrl =
    !person.profilePath || person.profilePath.endsWith("null")
      ? null
      : getTmdbImageUrl(person.profilePath, "profile", "h632");
  const facts = buildPersonFacts(person);

  return (
    <div className={ACTOR_PAGE_CLASS}>
      <section className={ACTOR_HERO_SECTION_CLASS}>
        <div className={ACTOR_CONTAINER_CLASS}>
          <div className={cn(ACTOR_GLASS_CLASS, ACTOR_GLASS_INNER_CLASS)}>
            <div className={ACTOR_HEADER_ROW_CLASS}>
              <div className={ACTOR_PROFILE_CLASS}>
                <img
                  src={profileUrl || missing}
                  alt={person.name}
                  className="size-full object-cover"
                  onError={(e) => {
                    e.currentTarget.src = missing;
                  }}
                />
              </div>
              <div className={ACTOR_INFO_CLASS}>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl md:text-5xl">
                  {person.name}
                </h1>
                {person.knownForDepartment && (
                  <p className="mt-2 text-lg text-muted-foreground">
                    {person.knownForDepartment}
                  </p>
                )}
                {facts.length > 0 && (
                  <dl className="mt-4 space-y-1 text-sm text-muted-foreground">
                    {facts.map((fact) => (
                      <div key={fact.label} className="flex flex-wrap gap-x-2">
                        <dt className="font-medium text-foreground">
                          {fact.label}
                        </dt>
                        <dd>{fact.value}</dd>
                      </div>
                    ))}
                  </dl>
                )}
                <BiographyBlock text={person.biography} />
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className={ACTOR_CONTAINER_CLASS}>
        <div className={ACTOR_TOOLBAR_CLASS}>
          <Tabs value={mediaTab} onValueChange={handleMediaTab}>
            <TabsList aria-label="Media type">
              {MEDIA_TABS.map((tab) => (
                <TabsTrigger key={tab.value} value={tab.value}>
                  {tab.label}
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>

          <div className="flex min-w-0 flex-wrap items-center gap-4">
            {departments.length > 0 && department != null && (
              <Select
                value={department}
                onValueChange={(value) => {
                  setDeptTouched(true);
                  setDepartment(value);
                }}
              >
                <SelectTrigger
                  aria-label="Department"
                  className="h-9 min-w-[10rem] border-border bg-card"
                >
                  <SelectValue placeholder="Department" />
                </SelectTrigger>
                <SelectContent>
                  {departments.map((dept) => (
                    <SelectItem key={dept} value={dept}>
                      {dept}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}

            <div className="flex items-center gap-2">
              <Switch
                id="show-appearances"
                checked={showAppearances}
                onCheckedChange={setShowAppearances}
              />
              <Label
                htmlFor="show-appearances"
                className="cursor-pointer text-sm text-foreground"
              >
                Show appearances
              </Label>
            </div>
          </div>
        </div>

        {groups.length === 0 ? (
          <p className="mt-10 text-muted-foreground">No credits to show.</p>
        ) : (
          groups.map((group) => (
            <section
              key={group.label}
              className={ACTOR_YEAR_GROUP_CLASS}
              aria-labelledby={`year-${group.label}`}
            >
              <h2
                id={`year-${group.label}`}
                className="mb-3 text-lg font-semibold text-foreground"
              >
                {group.label}
              </h2>
              <ul className="flex flex-col gap-2">
                {group.credits.map((item) => (
                  <li key={`${item.mediaType}-${item.tmdbId}`}>
                    <FilmographyCard item={item} />
                  </li>
                ))}
              </ul>
            </section>
          ))
        )}
      </div>
    </div>
  );
}

export default ActorPage;
