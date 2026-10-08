package com.atamanahmet.cinelog.client.tmdb;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenre;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenreListResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPersonResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.atamanahmet.cinelog.ratelimit.RateLimitBuckets;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TmdbClient {

    private static final String API_BASE = "https://api.themoviedb.org/3";

    private static final ParameterizedTypeReference<TmdbPagedResponse<TmdbMovieResponse>> MOVIE_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<TmdbPagedResponse<TmdbTvShowResponse>> TV_PAGE = new ParameterizedTypeReference<>() {
    };

    private final String apiKey;
    private final RestClient restClient;
    private final Bucket tmdbBucket;
    private final Duration maxWait;
    private final boolean retryOnUpstream429;
    private final Duration defaultRetryAfter;

    public TmdbClient(
            String apiKey,
            RestClient.Builder restClientBuilder,
            Bucket tmdbBucket,
            Duration maxWait,
            boolean retryOnUpstream429,
            Duration defaultRetryAfter) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.clone()
                .baseUrl(API_BASE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.tmdbBucket = tmdbBucket;
        this.maxWait = maxWait;
        this.retryOnUpstream429 = retryOnUpstream429;
        this.defaultRetryAfter = defaultRetryAfter;
    }

    /**
     * Discover movies using the existing TMDB query shape.
     */
    public TmdbPagedResponse<TmdbMovieResponse> discoverMovies(DiscoverRequest request) {
        String page = String.valueOf(request.page());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ReleaseDateWindow dates = Boolean.TRUE.equals(request.upcoming())
                ? ReleaseDateWindow.upcoming(today)
                : ReleaseDateWindow.released(request.yearRange(), today);
        return get("movie", page, uriBuilder -> {
            uriBuilder
                    .path("/discover/movie")
                    .queryParam("include_adult", request.adult())
                    .queryParam("include_video", "false")
                    .queryParam("page", page)
                    .queryParam("sort_by", request.sort().toTmdbSort(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE))
                    .queryParam("vote_count.gte", request.voteCount())
                    .queryParam("with_genres", request.genreIdsCsv())
                    .queryParam("primary_release_date.gte", dates.from().toString())
                    .queryParam("primary_release_date.lte", dates.to().toString())
                    .queryParam("vote_average.gte", request.ratingRange()[0])
                    .queryParam("vote_average.lte", request.ratingRange()[1])
                    .queryParam("with_original_language", request.languages());
            appendWithoutGenres(uriBuilder, request);
            appendRuntimeParams(uriBuilder, request);
            return uriBuilder.build();
        }, MOVIE_PAGE, null);
    }

    /**
     * Discover TV shows using the existing TMDB query shape.
     */
    public TmdbPagedResponse<TmdbTvShowResponse> discoverTvShows(DiscoverRequest request) {
        String page = String.valueOf(request.page());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ReleaseDateWindow dates = Boolean.TRUE.equals(request.upcoming())
                ? ReleaseDateWindow.upcoming(today)
                : ReleaseDateWindow.released(request.yearRange(), today);
        return get("tv", page, uriBuilder -> {
            uriBuilder
                    .path("/discover/tv")
                    .queryParam("include_adult", request.adult())
                    .queryParam("include_video", "false")
                    .queryParam("page", page)
                    .queryParam("sort_by", request.sort().toTmdbSort(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV))
                    .queryParam("vote_count.gte", request.voteCount())
                    .queryParam("with_genres", request.genreIdsCsv())
                    .queryParam("first_air_date.gte", dates.from().toString())
                    .queryParam("first_air_date.lte", dates.to().toString())
                    .queryParam("vote_average.gte", request.ratingRange()[0])
                    .queryParam("vote_average.lte", request.ratingRange()[1]);
            if (request.languages() != null && !request.languages().isBlank()) {
                uriBuilder.queryParam("with_original_language", request.languages());
            }
            appendWithoutGenres(uriBuilder, request);
            appendRuntimeParams(uriBuilder, request);
            return uriBuilder.build();
        }, TV_PAGE, null);
    }

    /**
     * Add without_genres only when exclude ids are present.
     */
    private static void appendWithoutGenres(UriBuilder uriBuilder, DiscoverRequest request) {
        String without = request.withoutGenresParam();
        if (!without.isEmpty()) {
            uriBuilder.queryParam("without_genres", without);
        }
    }

    /**
     * Add with_runtime.gte and with_runtime.lte only when those bounds are set.
     */
    private static void appendRuntimeParams(UriBuilder uriBuilder, DiscoverRequest request) {
        if (request.minRuntime() != null) {
            uriBuilder.queryParam("with_runtime.gte", request.minRuntime());
        }
        if (request.maxRuntime() != null) {
            uriBuilder.queryParam("with_runtime.lte", request.maxRuntime());
        }
    }

    /**
     * Search movies by free-text query and TMDB page number.
     */
    public TmdbPagedResponse<TmdbMovieResponse> searchMovies(String query, int page) {
        String pageValue = String.valueOf(page);
        return get("movie", query, uriBuilder -> uriBuilder
                .path("/search/movie")
                .queryParam("query", query)
                .queryParam("page", pageValue)
                .build(), MOVIE_PAGE, null);
    }

    /**
     * Search TV shows by free-text query and TMDB page number.
     */
    public TmdbPagedResponse<TmdbTvShowResponse> searchTvShows(String query, int page) {
        String pageValue = String.valueOf(page);
        return get("tv", query, uriBuilder -> uriBuilder
                .path("/search/tv")
                .queryParam("query", query)
                .queryParam("page", pageValue)
                .build(), TV_PAGE, null);
    }

    /**
     * Fetch one movie by TMDB id.
     */
    public TmdbMovieResponse getMovieById(Integer id) {
        return get("movie", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/movie/{id}").queryParam("append_to_response", "credits,videos")
                        .build(id),
                new ParameterizedTypeReference<TmdbMovieResponse>() {
                }, null);
    }

    /**
     * Fetch one TV show by TMDB id.
     */
    public TmdbTvShowResponse getTvShowById(Integer id) {
        return get("tv", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/tv/{id}").queryParam("append_to_response", "credits,videos")
                        .build(id),
                new ParameterizedTypeReference<TmdbTvShowResponse>() {
                }, null);
    }

    /**
     * Fetch light movie display fields with the client's default permit wait.
     */
    public MediaDisplay getMovieDisplayById(Integer id) {
        return toMovieDisplay(get("movie", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/movie/{id}").build(id),
                new ParameterizedTypeReference<TmdbMovieResponse>() {
                }, null));
    }

    /**
     * Fetch light movie display fields. Permit wait and 429 retry use the remaining deadline.
     */
    public MediaDisplay getMovieDisplayById(Integer id, Instant deadline) {
        return toMovieDisplay(get("movie", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/movie/{id}").build(id),
                new ParameterizedTypeReference<TmdbMovieResponse>() {
                }, deadline));
    }

    /**
     * Fetch light TV display fields with the client's default permit wait.
     */
    public MediaDisplay getTvDisplayById(Integer id) {
        return toTvDisplay(get("tv", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/tv/{id}").build(id),
                new ParameterizedTypeReference<TmdbTvShowResponse>() {
                }, null));
    }

    /**
     * Fetch light TV display fields. Permit wait and 429 retry use the remaining deadline.
     */
    public MediaDisplay getTvDisplayById(Integer id, Instant deadline) {
        return toTvDisplay(get("tv", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/tv/{id}").build(id),
                new ParameterizedTypeReference<TmdbTvShowResponse>() {
                }, deadline));
    }

    /**
     * Fetch one person by TMDB id with combined credits appended.
     */
    public TmdbPersonResponse getPersonById(Integer id) {
        return get("person", String.valueOf(id),
                uriBuilder -> uriBuilder.path("/person/{id}")
                        .queryParam("append_to_response", "combined_credits")
                        .build(id),
                new ParameterizedTypeReference<TmdbPersonResponse>() {
                }, null);
    }

    /**
     * Fetch the full movie genre list from TMDB.
     */
    public TmdbGenreListResponse getMovieGenres() {
        return get("movie", "genres",
                uriBuilder -> uriBuilder.path("/genre/movie/list").build(),
                new ParameterizedTypeReference<TmdbGenreListResponse>() {
                }, null);
    }

    /**
     * Fetch the full TV genre list from TMDB.
     */
    public TmdbGenreListResponse getTvGenres() {
        return get("tv", "genres",
                uriBuilder -> uriBuilder.path("/genre/tv/list").build(),
                new ParameterizedTypeReference<TmdbGenreListResponse>() {
                }, null);
    }

    /**
     * Earlier of 31 December of endYear and today. Null endYear means today.
     */
    static String upperReleaseBound(Integer endYear, LocalDate today) {
        if (endYear == null) {
            return today.toString();
        }
        LocalDate endOfSelectedYear = LocalDate.of(endYear, 12, 31);
        if (endOfSelectedYear.isBefore(today)) {
            return endOfSelectedYear.toString();
        }
        return today.toString();
    }

    /**
     * Map a movie detail body to MediaDisplay.
     */
    static MediaDisplay toMovieDisplay(TmdbMovieResponse response) {
        if (response == null || response.getId() == null) {
            throw new TmdbClientException("movie", "display", "TMDB movie body missing id", null, -1);
        }
        String title = response.getTitle();
        if (title == null || title.isBlank()) {
            throw new TmdbClientException("movie", String.valueOf(response.getId()),
                    "TMDB movie body missing title", null, -1);
        }
        return new MediaDisplay(
                response.getId(),
                TmdbMediaType.MOVIE,
                title,
                response.getPosterPath(),
                parseReleaseDate(response.getReleaseDate()),
                response.getOverview(),
                response.getVoteAverage(),
                response.getVoteCount(),
                toGenreIds(response.getGenres()));
    }

    /**
     * Map a TV detail body to MediaDisplay.
     */
    static MediaDisplay toTvDisplay(TmdbTvShowResponse response) {
        if (response == null || response.getId() == null) {
            throw new TmdbClientException("tv", "display", "TMDB tv body missing id", null, -1);
        }
        String title = response.getTitle();
        if (title == null || title.isBlank()) {
            throw new TmdbClientException("tv", String.valueOf(response.getId()),
                    "TMDB tv body missing title", null, -1);
        }
        return new MediaDisplay(
                response.getId(),
                TmdbMediaType.TV,
                title,
                response.getPosterPath(),
                parseReleaseDate(response.getReleaseDate()),
                response.getOverview(),
                response.getVoteAverage(),
                response.getVoteCount(),
                toGenreIds(response.getGenres()));
    }

    /**
     * Collect genre ids from a TMDB genres array. Missing or null becomes empty.
     */
    static List<Integer> toGenreIds(List<TmdbGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }
        return genres.stream().map(TmdbGenre::id).toList();
    }

    /**
     * Parse an ISO date string. Blank or bad values become null.
     */
    static LocalDate parseReleaseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Read Retry-After seconds. Missing or non-positive values use the configured default.
     */
    static Duration parseRetryAfter(HttpHeaders headers, Duration defaultRetryAfter) {
        if (headers == null) {
            return defaultRetryAfter;
        }
        String raw = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (raw == null || raw.isBlank()) {
            return defaultRetryAfter;
        }
        try {
            long seconds = Long.parseLong(raw.trim());
            if (seconds <= 0) {
                return defaultRetryAfter;
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            return defaultRetryAfter;
        }
    }

    /**
     * Remaining time until deadline, or Duration.ZERO when the deadline has passed.
     */
    static Duration remainingUntil(Instant deadline) {
        if (deadline == null) {
            return null;
        }
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative()) {
            return Duration.ZERO;
        }
        return remaining;
    }

    /**
     * Take one outbound TMDB permit using waitBudget (or maxWait when null).
     */
    private void acquireToken(String endpoint, String mediaType, String requestContext, Duration waitBudget) {
        Duration wait = waitBudget != null ? waitBudget : maxWait;
        if (wait.isZero() || wait.isNegative()) {
            ConsumptionProbe probe = RateLimitBuckets.tryAcquire(tmdbBucket);
            if (!probe.isConsumed()) {
                log.debug("TMDB rate limit exhausted endpoint={} mediaType={} context={}", endpoint, mediaType,
                        requestContext);
                throw new TmdbRateLimitExceededException(probe.getNanosToWaitForRefill());
            }
            return;
        }
        try {
            if (!RateLimitBuckets.tryAcquire(tmdbBucket, wait)) {
                throw new TmdbRateLimitExceededException(wait.toNanos());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TmdbClientException(mediaType, requestContext, "The wait for a TMDB token was interrupted", e);
        }
    }

    private <T> T get(
            String mediaType,
            String requestContext,
            Function<UriBuilder, URI> uriFunction,
            ParameterizedTypeReference<T> type,
            Instant deadline) {
        URI requestUri = uriFunction.apply(new DefaultUriBuilderFactory(API_BASE).builder());
        String endpoint = requestUri.getPath();
        Duration waitBudget = deadline != null ? remainingUntil(deadline) : null;
        if (deadline != null && (waitBudget == null || waitBudget.isZero())) {
            throw new TmdbRateLimitExceededException(0L);
        }
        acquireToken(endpoint, mediaType, requestContext, waitBudget);
        try {
            return exchange(requestUri, endpoint, type);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", e,
                        e.getStatusCode().value());
            }
            if (!retryOnUpstream429 || deadline == null) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", e,
                        e.getStatusCode().value());
            }
            Duration retryAfter = parseRetryAfter(e.getResponseHeaders(), defaultRetryAfter);
            Duration remaining = remainingUntil(deadline);
            if (remaining == null || remaining.compareTo(retryAfter) < 0) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", e,
                        e.getStatusCode().value());
            }
            try {
                Thread.sleep(retryAfter.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new TmdbClientException(mediaType, requestContext,
                        "The wait for TMDB Retry-After was interrupted", interrupted);
            }
            Duration retryWait = remainingUntil(deadline);
            if (retryWait == null || retryWait.isZero()) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", e,
                        e.getStatusCode().value());
            }
            acquireToken(endpoint, mediaType, requestContext, retryWait);
            try {
                return exchange(requestUri, endpoint, type);
            } catch (RestClientResponseException retryError) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", retryError,
                        retryError.getStatusCode().value());
            } catch (RestClientException retryError) {
                throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", retryError, -1);
            }
        } catch (RestClientException e) {
            throw new TmdbClientException(mediaType, requestContext, "TMDB request failed", e, -1);
        }
    }

    /**
     * Perform one GET against TMDB and return the body.
     */
    private <T> T exchange(URI requestUri, String endpoint, ParameterizedTypeReference<T> type) {
        ResponseEntity<T> response = restClient.get()
                .uri(requestUri)
                .retrieve()
                .toEntity(type);
        log.debug("TMDB status={} endpoint={}", response.getStatusCode(), endpoint);
        return response.getBody();
    }
}
