import { Star, Calendar, Users, Globe, Play } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router";
import { useState } from "react";
import VideoModal from "../components/VideoModal";
import ListActionButton from "../components/ListActionButton";
import missing from "../assets/missing.png";
import useDetails from "../hooks/useDetails";
import { mediaReleaseDate } from "../utils/media";
import { backdropSrcSet, getTmdbImageUrl } from "../lib/tmdbImage";
import { labeledGenres } from "../lib/genreLabels";
import { usePageFiltersStore } from "../stores/pageFiltersStore";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "../stores/authStore";
import useAdultPolicy from "../hooks/useAdultPolicy";
import { cn } from "@/lib/utils";
import DetailsHero from "../components/DetailsHero";
import TvSeriesInfoSection from "../components/TvSeriesInfoSection";
import { formatDisplayDate } from "../lib/displayDate";
import DetailsPageSkeleton from "./DetailsPageSkeleton";
import {
  DETAILS_CAST_GRID_CLASS,
  DETAILS_CAST_SECTION_CLASS,
  DETAILS_CONTAINER_CLASS,
  DETAILS_INFO_CLASS,
  DETAILS_PAGE_CLASS,
  DETAILS_POSTER_CLASS,
  DETAILS_ROW_CLASS,
} from "./detailsLayout";

/**
 * Display title. Same JSON key for movie and TV.
 */
function mediaTitle(item) {
  return item?.title;
}

/**
 * Original title. Same JSON key for movie and TV.
 */
function mediaOriginalTitle(item) {
  return item?.originalTitle;
}

/**
 * Rating colour by vote average.
 */
function getRatingColor(rating) {
  if (rating >= 8) return "text-foreground";
  if (rating >= 6) return "text-accent";
  return "text-destructive";
}

/**
 * True when a TMDB path can be requested.
 */
function hasImagePath(path) {
  return typeof path === "string" && path !== "" && !path.endsWith("null");
}

/**
 * Image that fades in. Missing path or a failed load shows the fallback.
 */
function FadeImage({ src, srcSet, sizes, alt, className, fallback }) {
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);

  if (!src || failed) {
    return <img src={fallback} alt={alt} className={className} />;
  }

  return (
    <img
      src={src}
      srcSet={srcSet || undefined}
      sizes={srcSet ? sizes : undefined}
      alt={alt}
      onLoad={() => setLoaded(true)}
      onError={() => setFailed(true)}
      className={cn(
        className,
        "transition-opacity duration-300 motion-reduce:transition-none",
        loaded ? "opacity-100" : "opacity-0",
      )}
    />
  );
}

/**
 * Movie or TV details.
 */
function DetailsPage() {
  const user = useAuthStore((s) => s.user);
  const { mediaType, id } = useParams();
  const navigate = useNavigate();
  const validType = mediaType === "movie" || mediaType === "tv";
  const { details, cast, trailerUrl, loading, error, retry } = useDetails(
    validType ? mediaType : null,
    id,
  );
  const detail = details;
  const title = mediaTitle(detail);
  const originalTitle = mediaOriginalTitle(detail);
  const dateValue = mediaReleaseDate(detail);

  const [isVideoModalOpen, setIsVideoModalOpen] = useState(false);
  const { blurAdult } = useAdultPolicy();
  const applyGenreEntry = usePageFiltersStore((s) => s.applyGenreEntry);
  const blurClass = detail?.adult && blurAdult ? "scale-110 blur-2xl" : "";

  const openVideoModal = () => {
    setIsVideoModalOpen(true);
  };

  const closeVideoModal = () => {
    setIsVideoModalOpen(false);
  };

  if (!validType) {
    return (
      <div className="min-h-screen bg-background pt-20 text-center text-foreground">
        <p className="mb-6 text-lg">Not found</p>
        <button
          className="rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
          onClick={() => navigate(-1)}
        >
          Back
        </button>
      </div>
    );
  }

  if (error === "not-found") {
    return (
      <div className="min-h-screen bg-background pt-20 text-center text-foreground">
        <p className="mb-6 text-lg">Title not found</p>
        <Link
          to="/"
          className="rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          Discover
        </Link>
      </div>
    );
  }

  if (error && !details) {
    return (
      <div className="min-h-screen bg-background pt-20 text-center text-foreground">
        <p className="mb-6 text-lg">Could not load details</p>
        <div className="flex flex-wrap justify-center gap-4">
          <button
            className="rounded-lg bg-secondary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
            onClick={() => navigate(-1)}
          >
            Back
          </button>
          <button
            className="rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
            onClick={retry}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (loading || !detail) {
    return <DetailsPageSkeleton />;
  }

  const backdropPath = hasImagePath(detail.backdropPath)
    ? detail.backdropPath
    : "";
  const posterPath = hasImagePath(detail.posterPath) ? detail.posterPath : "";
  const genreChips = labeledGenres(detail.genreIds);
  const imageAlt = title ?? "";

  const backdropUrl = backdropPath
    ? getTmdbImageUrl(backdropPath, "backdrop", "w1280")
    : "";

  return (
    <div className={DETAILS_PAGE_CLASS}>
      <DetailsHero
        backdropSrc={backdropUrl || undefined}
        backdropSrcSet={backdropPath ? backdropSrcSet(backdropPath) : undefined}
        backdropSizes="100vw"
        backdropAlt={imageAlt}
        backdropFallback={missing}
        backdropImageClassName={blurClass}
      >
        <div className={DETAILS_ROW_CLASS}>
          <div className={DETAILS_POSTER_CLASS}>
            <FadeImage
                key={posterPath || "poster-missing"}
                src={
                  posterPath
                    ? getTmdbImageUrl(posterPath, "poster", "w500")
                    : ""
                }
                alt={imageAlt}
                fallback={missing}
                className={cn("size-full object-cover", blurClass)}
              />
              {blurClass && (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-foreground/30 text-background">
                  <span className="rounded-md bg-destructive px-2 py-0.5 font-bold text-primary-foreground">
                    18+
                  </span>
                  <span className="text-sm font-medium">Adult content</span>
                </div>
              )}
          </div>

          <div className={DETAILS_INFO_CLASS}>
              <div className="mb-4">
                <h1 className="mb-2 text-4xl font-bold md:text-6xl">
                  {title}
                </h1>
                {originalTitle && originalTitle !== title && (
                  <p className="text-xl italic text-muted-foreground">
                    {originalTitle}
                  </p>
                )}
              </div>

              <div className="mb-6 flex flex-wrap items-center gap-6">
                {detail.voteAverage != null && (
                  <div className="flex items-center gap-2">
                    <Star className="h-5 w-5 fill-current text-accent" />
                    <span
                      className={cn(
                        "text-lg font-semibold",
                        getRatingColor(detail.voteAverage),
                      )}
                    >
                      {detail.voteAverage.toFixed(1)}
                    </span>
                    {detail.voteCount != null && (
                      <span className="text-foreground/80 dark:text-muted-foreground">
                        ({detail.voteCount.toLocaleString()} votes)
                      </span>
                    )}
                  </div>
                )}

                {formatDisplayDate(dateValue) && (
                  <div className="flex items-center gap-2">
                    <Calendar className="h-5 w-5 text-foreground dark:text-muted-foreground" />
                    <span>{formatDisplayDate(dateValue)}</span>
                  </div>
                )}

                {detail.originalLanguage && (
                  <div className="flex items-center gap-2">
                    <Globe className="h-5 w-5 text-foreground dark:text-muted-foreground" />
                    <span className="uppercase">{detail.originalLanguage}</span>
                  </div>
                )}

                {detail.adult && (
                  <div className="rounded bg-destructive px-2 py-1 text-sm font-semibold text-primary-foreground">
                    18+
                  </div>
                )}
              </div>

              {genreChips.length > 0 && (
                <div className="mb-6">
                  <div className="flex flex-wrap gap-2">
                    {genreChips.map(({ id, name }) => (
                      <Button
                        key={id}
                        asChild
                        variant="secondary"
                        className="h-auto rounded-full px-3 py-1 text-sm shadow-none hover:opacity-80"
                      >
                        <Link
                          to="/"
                          aria-label={
                            mediaType === "tv"
                              ? `Discover ${name} TV shows`
                              : `Discover ${name} movies`
                          }
                          onClick={() => applyGenreEntry(mediaType, id)}
                        >
                          {name}
                        </Link>
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              <div className="mb-8">
                <h2 className="mb-4 text-2xl font-semibold">Overview</h2>
                <p className="max-w-4xl text-lg leading-relaxed text-foreground/90">
                  {detail.overview}
                </p>
              </div>

              <div className="flex flex-wrap gap-4">
                <button
                  className="flex items-center gap-2 rounded-lg bg-primary px-6 py-3 font-semibold transition-colors hover:bg-accent hover:text-accent-foreground"
                  onClick={openVideoModal}
                  disabled={!trailerUrl}
                >
                  <Play className="h-5 w-5" />
                  {trailerUrl != null
                    ? "Watch Trailer"
                    : "No Trailer Available"}
                </button>
                {user && (
                  <div className="flex items-center gap-2">
                    <ListActionButton
                      item={detail}
                      listType="watchlist"
                      mediaType={mediaType}
                    />
                    <ListActionButton
                      item={detail}
                      listType="watched"
                      mediaType={mediaType}
                    />
                    <ListActionButton
                      item={detail}
                      listType="loved"
                      mediaType={mediaType}
                    />
                  </div>
                )}
              </div>
          </div>
        </div>
      </DetailsHero>

      <div className={cn(DETAILS_CONTAINER_CLASS, "min-w-0 overflow-x-hidden")}>
        {mediaType === "tv" && <TvSeriesInfoSection detail={detail} />}
        {cast.length > 0 && (
          <div className={DETAILS_CAST_SECTION_CLASS}>
            <h2 className="mb-6 flex items-center gap-2 text-2xl font-semibold">
              <Users className="h-6 w-6 text-muted-foreground" />
              Cast
            </h2>
            <div className={DETAILS_CAST_GRID_CLASS}>
              {cast.map((actor) => (
                <Link
                  key={actor.id}
                  to={`/person/${actor.id}`}
                  aria-label={`${actor.name}, actor`}
                  className="text-center transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <div className="relative mb-3">
                    <img
                      src={
                        !actor.profile_path ||
                        actor.profile_path.endsWith("null")
                          ? missing
                          : getTmdbImageUrl(
                              actor.profile_path,
                              "profile",
                              "w185",
                            )
                      }
                      alt=""
                      className="aspect-square w-full rounded-full bg-muted object-cover shadow-lg"
                      onError={(e) => {
                        e.target.src = missing;
                      }}
                    />
                  </div>
                  <h3 className="mb-1 truncate text-sm font-semibold text-foreground">
                    {actor.name}
                  </h3>
                  <p className="truncate text-xs text-muted-foreground">
                    {actor.character || actor.job}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        )}
        <div className="mt-8 grid grid-cols-2 gap-4 md:grid-cols-4">
          {detail.popularity != null && (
            <div className="rounded-lg bg-card p-4 text-center">
              <div className="text-2xl font-bold text-primary">
                {detail.popularity.toFixed(0)}
              </div>
              <div className="text-sm text-muted-foreground">Popularity</div>
            </div>
          )}
          {detail.voteCount != null && (
            <div className="rounded-lg bg-card p-4 text-center">
              <div className="text-2xl font-bold text-accent">
                {detail.voteCount.toLocaleString()}
              </div>
              <div className="text-sm text-muted-foreground">Reviews</div>
            </div>
          )}
          {detail.id != null && (
            <div className="rounded-lg bg-card p-4 text-center">
              <div className="text-2xl font-bold text-foreground">
                #{detail.id}
              </div>
              <div className="text-sm text-muted-foreground">detail ID</div>
            </div>
          )}
          {detail.video !== undefined && (
            <div className="rounded-lg bg-card p-4 text-center">
              <div className="text-2xl font-bold text-muted-foreground">
                {detail.video ? "Yes" : "No"}
              </div>
              <div className="text-sm text-muted-foreground">Has Video</div>
            </div>
          )}
        </div>
      </div>
      <VideoModal
        isOpen={isVideoModalOpen}
        onClose={closeVideoModal}
        videoUrl={trailerUrl}
        title={title}
      />
    </div>
  );
}

export default DetailsPage;
