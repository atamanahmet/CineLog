package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.client.tmdb.TmdbRateLimitExceededException;
import com.atamanahmet.cinelog.config.RecommendationCacheProperties;
import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class RecommendationHydratorTest {

    private static final String API_KEY = "test-tmdb-token-xxxxxxxxxxxxxxxx";
    private static final Duration RETRY_AFTER_DEFAULT = Duration.ofSeconds(1);

    @Mock
    private CatalogCacheRepository catalogCacheRepository;

    @Mock
    private TmdbClient tmdbClient;

    private Cache<MediaKey, MediaDisplay> displayCache;
    private ExecutorService executor;
    private RecommendationCacheProperties properties;
    private RecommendationQualityFilter qualityFilter;
    private RecommendationHydrator hydrator;

    @BeforeEach
    void setUp() {
        displayCache = Caffeine.newBuilder().maximumSize(100).build();
        executor = Executors.newFixedThreadPool(2);
        properties = new RecommendationCacheProperties();
        properties.setHydrateTimeoutSeconds(2);
        qualityFilter = new RecommendationQualityFilter(new RecommendationQualityProperties());
        hydrator = new RecommendationHydrator(
                catalogCacheRepository, tmdbClient, displayCache, executor, properties, qualityFilter);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /**
     * Memory hit skips catalog and TMDB for that key.
     */
    @Test
    void memoryHitSkipsCatalogAndTmdb() {
        MediaKey key = new MediaKey(1, TmdbMediaType.MOVIE);
        displayCache.put(key, new MediaDisplay(1, TmdbMediaType.MOVIE, "Cached", "/c.jpg", null, "o", 8.1, null, List.of()));

        List<RecommendationItemDTO> items = hydrator.hydrate(List.of(key), Map.of(key, 0.9));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("Cached");
        assertThat(items.get(0).score()).isEqualTo(0.9);
        assertThat(items.get(0).voteAverage()).isEqualTo(8.1);
        verify(catalogCacheRepository, never()).findByMediaTypeAndTmdbIdIn(any(), anyCollection());
        verify(tmdbClient, never()).getMovieDisplayById(anyInt(), any(Instant.class));
    }

    /**
     * Tier order is memory, then catalog_cache, then TMDB; scores and order stay.
     */
    @Test
    void tierOrderKeepsScoresAndOrder() {
        MediaKey memoryKey = new MediaKey(1, TmdbMediaType.MOVIE);
        MediaKey catalogKey = new MediaKey(2, TmdbMediaType.MOVIE);
        MediaKey tmdbKey = new MediaKey(3, TmdbMediaType.TV);
        displayCache.put(memoryKey, new MediaDisplay(1, TmdbMediaType.MOVIE, "Mem", null, null, null, 7.0, null, List.of()));

        CatalogCache cache = new CatalogCache();
        cache.setTmdbId(2);
        cache.setMediaType(TmdbMediaType.MOVIE);
        cache.setTitle("Catalog");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of(cache));
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), anyCollection()))
                .thenReturn(List.of());
        when(tmdbClient.getTvDisplayById(eq(3), any(Instant.class)))
                .thenReturn(new MediaDisplay(
                        3,
                        TmdbMediaType.TV,
                        "Tmdb",
                        null,
                        LocalDate.of(2020, 1, 1),
                        "ov",
                        6.5,
                        null,
                        List.of()));

        List<RecommendationItemDTO> items = hydrator.hydrate(
                List.of(memoryKey, catalogKey, tmdbKey),
                Map.of(memoryKey, 0.9, catalogKey, 0.8, tmdbKey, 0.7));

        assertThat(items).extracting(RecommendationItemDTO::id).containsExactly(1, 2, 3);
        assertThat(items).extracting(RecommendationItemDTO::score).containsExactly(0.9, 0.8, 0.7);
        assertThat(items).extracting(RecommendationItemDTO::voteAverage).containsExactly(7.0, null, 6.5);
        assertThat(displayCache.getIfPresent(tmdbKey)).isNotNull();
        assertThat(displayCache.getIfPresent(catalogKey)).isNull();
        verify(catalogCacheRepository, never()).save(any());
        verify(tmdbClient, times(1)).getTvDisplayById(eq(3), any(Instant.class));
        verify(tmdbClient, never()).getMovieDisplayById(anyInt(), any(Instant.class));
    }

    /**
     * One TMDB failure still returns the other rows.
     */
    @Test
    void partialTmdbFailureStillReturnsOthers() {
        MediaKey ok = new MediaKey(1, TmdbMediaType.MOVIE);
        MediaKey bad = new MediaKey(2, TmdbMediaType.MOVIE);
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of());
        when(tmdbClient.getMovieDisplayById(eq(1), any(Instant.class)))
                .thenReturn(new MediaDisplay(1, TmdbMediaType.MOVIE, "Ok", null, null, null, null, null, List.of()));
        when(tmdbClient.getMovieDisplayById(eq(2), any(Instant.class)))
                .thenThrow(new TmdbClientException("movie", "2", "TMDB request failed", null, 500));

        List<RecommendationItemDTO> items = hydrator.hydrate(List.of(ok, bad), Map.of(ok, 0.5, bad, 0.4));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).id()).isEqualTo(1);
    }

    /**
     * Timed out TMDB keys are skipped and no task keeps running after return.
     */
    @Test
    void timeoutSkipsSlowKeysAndDrainsTasks() throws Exception {
        properties.setHydrateTimeoutSeconds(1);
        AtomicInteger inFlight = new AtomicInteger();
        MediaKey slow = new MediaKey(1, TmdbMediaType.MOVIE);
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of());
        when(tmdbClient.getMovieDisplayById(eq(1), any(Instant.class))).thenAnswer(invocation -> {
            inFlight.incrementAndGet();
            try {
                Thread.sleep(5000);
                return new MediaDisplay(1, TmdbMediaType.MOVIE, "Late", null, null, null, null, null, List.of());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TmdbClientException("movie", "1", "interrupted", e);
            } finally {
                inFlight.decrementAndGet();
            }
        });

        List<RecommendationItemDTO> items = hydrator.hydrate(List.of(slow), Map.of(slow, 0.9));

        assertThat(items).isEmpty();
        assertThat(inFlight.get()).isZero();
        assertThat(executor.awaitTermination(0, TimeUnit.MILLISECONDS)).isFalse();
    }

    /**
     * hydrateCacheOnly never calls TMDB.
     */
    @Test
    void hydrateCacheOnlyNeverCallsTmdb() {
        MediaKey key = new MediaKey(9, TmdbMediaType.TV);
        CatalogCache cache = new CatalogCache();
        cache.setTmdbId(9);
        cache.setMediaType(TmdbMediaType.TV);
        cache.setTitle("Show");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), anyCollection()))
                .thenReturn(List.of(cache));

        List<RecommendationItemDTO> items = hydrator.hydrateCacheOnly(List.of(key));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).originalTitle()).isEqualTo("Show");
        assertThat(items.get(0).voteAverage()).isNull();
        verify(tmdbClient, never()).getTvDisplayById(anyInt(), any(Instant.class));
        verify(tmdbClient, never()).getMovieDisplayById(anyInt(), any(Instant.class));
    }

    /**
     * TMDB display genre ids flow into the recommendation item DTO.
     */
    @Test
    void itemCarriesGenreIdsFromDisplay() {
        MediaKey key = new MediaKey(1, TmdbMediaType.MOVIE);
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of());
        when(tmdbClient.getMovieDisplayById(eq(1), any(Instant.class)))
                .thenReturn(new MediaDisplay(
                        1,
                        TmdbMediaType.MOVIE,
                        "Heat",
                        null,
                        null,
                        null,
                        7.9,
                        null,
                        List.of(28, 80)));

        List<RecommendationItemDTO> items = hydrator.hydrate(List.of(key), Map.of(key, 0.9));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).genreIds()).containsExactly(28, 80);
    }

    /**
     * Catalog cache hits have no genres, so genreIds is empty.
     */
    @Test
    void catalogHitGivesEmptyGenreIds() {
        MediaKey key = new MediaKey(2, TmdbMediaType.MOVIE);
        CatalogCache cache = new CatalogCache();
        cache.setTmdbId(2);
        cache.setMediaType(TmdbMediaType.MOVIE);
        cache.setTitle("Catalog");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of(cache));

        List<RecommendationItemDTO> items = hydrator.hydrate(List.of(key), Map.of(key, 0.8));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).genreIds()).isEmpty();
        verify(tmdbClient, never()).getMovieDisplayById(anyInt(), any(Instant.class));
    }

    /**
     * Cache bound and TTL come from the properties object.
     */
    @Test
    void cachePropertiesDefaults() {
        RecommendationCacheProperties defaults = new RecommendationCacheProperties();
        assertThat(defaults.getMaxSize()).isEqualTo(5000);
        assertThat(defaults.getTtlHours()).isEqualTo(6);
        assertThat(defaults.getHydratePoolSize()).isEqualTo(8);
        assertThat(defaults.getHydrateTimeoutSeconds()).isEqualTo(8);
    }

    /**
     * Fifty ids with capacity 25 all hydrate; the second half waits for refill.
     */
    @Test
    void fiftyIdsWithCapacityTwentyFiveAllHydrate() {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(25)
                            .refillGreedy(25, Duration.ofSeconds(1))
                            .build())
                    .build();
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
            TmdbClient waitingClient = new TmdbClient(
                    API_KEY, builder, bucket, Duration.ofSeconds(8), true, RETRY_AFTER_DEFAULT);
            properties.setHydrateTimeoutSeconds(8);
            hydrator = new RecommendationHydrator(
                    catalogCacheRepository, waitingClient, displayCache, pool, properties, qualityFilter);

            List<MediaKey> keys = new ArrayList<>();
            Map<MediaKey, Double> scores = new HashMap<>();
            when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                    .thenReturn(List.of());
            for (int id = 1; id <= 50; id++) {
                MediaKey key = new MediaKey(id, TmdbMediaType.MOVIE);
                keys.add(key);
                scores.put(key, 1.0 - (id * 0.001));
                stubMovie(server, id, "Title-" + id);
            }

            List<RecommendationItemDTO> items = hydrator.hydrate(keys, scores);

            assertThat(items).hasSize(50);
            server.verify();
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Wait stops at the deadline; remaining ids are skipped; no task still runs after return.
     */
    @Test
    void waitStopsAtDeadlineAndDrains() throws Exception {
        ExecutorService single = Executors.newSingleThreadExecutor();
        AtomicInteger activeAcquires = new AtomicInteger();
        try {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(1)
                            .refillGreedy(1, Duration.ofSeconds(60))
                            .build())
                    .build();
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            TmdbClient waitingClient = new TmdbClient(
                    API_KEY, builder, bucket, Duration.ofSeconds(8), true, RETRY_AFTER_DEFAULT) {
                @Override
                public MediaDisplay getMovieDisplayById(Integer id, Instant deadline) {
                    activeAcquires.incrementAndGet();
                    try {
                        return super.getMovieDisplayById(id, deadline);
                    } finally {
                        activeAcquires.decrementAndGet();
                    }
                }
            };
            properties.setHydrateTimeoutSeconds(1);
            hydrator = new RecommendationHydrator(
                    catalogCacheRepository, waitingClient, displayCache, single, properties, qualityFilter);

            MediaKey ok = new MediaKey(1, TmdbMediaType.MOVIE);
            MediaKey skipped = new MediaKey(2, TmdbMediaType.MOVIE);
            when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                    .thenReturn(List.of());
            stubMovie(server, 1, "Ok");

            List<RecommendationItemDTO> items = hydrator.hydrate(
                    List.of(ok, skipped), Map.of(ok, 0.9, skipped, 0.8));

            assertThat(items).hasSize(1);
            assertThat(items.get(0).title()).isEqualTo("Ok");
            assertThat(activeAcquires.get()).isZero();
            server.verify();
        } finally {
            single.shutdownNow();
            assertThat(single.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    /**
     * Rate-limit and 429 skips produce one summary WARN, not one WARN stack per id.
     */
    @Test
    void summaryLogOnceForRateLimitAnd429Skips() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecommendationHydrator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level previous = logger.getLevel();
        logger.setLevel(Level.WARN);
        try {
            MediaKey ok = new MediaKey(1, TmdbMediaType.MOVIE);
            MediaKey limited = new MediaKey(2, TmdbMediaType.MOVIE);
            MediaKey upstream429 = new MediaKey(3, TmdbMediaType.MOVIE);
            when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                    .thenReturn(List.of());
            when(tmdbClient.getMovieDisplayById(eq(1), any(Instant.class)))
                    .thenReturn(new MediaDisplay(1, TmdbMediaType.MOVIE, "Ok", null, null, null, null, null, List.of()));
            when(tmdbClient.getMovieDisplayById(eq(2), any(Instant.class)))
                    .thenThrow(new TmdbRateLimitExceededException(1_000_000_000L));
            when(tmdbClient.getMovieDisplayById(eq(3), any(Instant.class)))
                    .thenThrow(new TmdbClientException("movie", "3", "TMDB request failed", null, 429));

            List<RecommendationItemDTO> items = hydrator.hydrate(
                    List.of(ok, limited, upstream429),
                    Map.of(ok, 0.9, limited, 0.8, upstream429, 0.7));

            assertThat(items).hasSize(1);
            List<ILoggingEvent> warns = appender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .toList();
            assertThat(warns).hasSize(1);
            assertThat(warns.get(0).getFormattedMessage())
                    .contains("Recommendation hydrate requested=3")
                    .contains("fromTmdb=1")
                    .contains("skippedRateLimit=1")
                    .contains("skippedUpstream429=1")
                    .contains("filteredLowVotes=0")
                    .doesNotContain("TMDB display failed");
            assertThat(warns.get(0).getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }

    /**
     * Low-vote rows leave the returned list but stay in the memory cache.
     */
    @Test
    void filterDropsFromResultButKeepsMemoryCache() {
        MediaKey low = new MediaKey(1, TmdbMediaType.MOVIE);
        MediaKey ok = new MediaKey(2, TmdbMediaType.MOVIE);
        displayCache.put(low, new MediaDisplay(
                1, TmdbMediaType.MOVIE, "Low", null, null, null, 6.0, 10, List.of()));
        displayCache.put(ok, new MediaDisplay(
                2, TmdbMediaType.MOVIE, "Ok", null, null, null, 8.0, 100, List.of()));

        List<RecommendationItemDTO> items = hydrator.hydrate(
                List.of(low, ok), Map.of(low, 0.9, ok, 0.8));

        assertThat(items).extracting(RecommendationItemDTO::id).containsExactly(2);
        assertThat(displayCache.getIfPresent(low)).isNotNull();
        assertThat(displayCache.getIfPresent(low).voteCount()).isEqualTo(10);
        assertThat(displayCache.getIfPresent(ok)).isNotNull();
    }

    /**
     * Item DTO carries vote_count from the display and null when the display has none.
     */
    @Test
    void itemCarriesVoteCountFromDisplay() {
        MediaKey withVotes = new MediaKey(1, TmdbMediaType.MOVIE);
        MediaKey without = new MediaKey(2, TmdbMediaType.MOVIE);
        displayCache.put(withVotes, new MediaDisplay(
                1, TmdbMediaType.MOVIE, "With", null, null, null, 7.0, 250, List.of()));
        displayCache.put(without, new MediaDisplay(
                2, TmdbMediaType.MOVIE, "Without", null, null, null, 7.0, null, List.of()));

        List<RecommendationItemDTO> items = hydrator.hydrate(
                List.of(withVotes, without), Map.of(withVotes, 0.9, without, 0.8));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).voteCount()).isEqualTo(250);
        assertThat(items.get(1).voteCount()).isNull();
    }

    /**
     * All-memory hydrate that drops low-vote rows still writes a summary with filteredLowVotes.
     */
    @Test
    void summaryFiresForAllMemoryDrops() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecommendationHydrator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level previous = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            MediaKey low = new MediaKey(1, TmdbMediaType.MOVIE);
            displayCache.put(low, new MediaDisplay(
                    1, TmdbMediaType.MOVIE, "Low", null, null, null, 5.0, 5, List.of()));

            List<RecommendationItemDTO> items = hydrator.hydrate(List.of(low), Map.of(low, 0.9));

            assertThat(items).isEmpty();
            List<ILoggingEvent> infos = appender.list.stream()
                    .filter(event -> event.getLevel() == Level.INFO)
                    .filter(event -> event.getFormattedMessage().contains("Recommendation hydrate"))
                    .toList();
            assertThat(infos).hasSize(1);
            assertThat(infos.get(0).getFormattedMessage())
                    .contains("fromMemory=1")
                    .contains("fromTmdb=0")
                    .contains("filteredLowVotes=1");
            assertThat(appender.list.stream().filter(event -> event.getLevel() == Level.WARN)).isEmpty();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }

    /**
     * TMDB path with no real skips logs the summary at INFO.
     */
    @Test
    void summaryInfoWhenNoRealSkips() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecommendationHydrator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level previous = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            MediaKey key = new MediaKey(1, TmdbMediaType.MOVIE);
            when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                    .thenReturn(List.of());
            when(tmdbClient.getMovieDisplayById(eq(1), any(Instant.class)))
                    .thenReturn(new MediaDisplay(
                            1, TmdbMediaType.MOVIE, "Ok", null, null, null, 8.0, 200, List.of()));

            List<RecommendationItemDTO> items = hydrator.hydrate(List.of(key), Map.of(key, 0.9));

            assertThat(items).hasSize(1);
            List<ILoggingEvent> infos = appender.list.stream()
                    .filter(event -> event.getLevel() == Level.INFO)
                    .filter(event -> event.getFormattedMessage().contains("Recommendation hydrate"))
                    .toList();
            assertThat(infos).hasSize(1);
            assertThat(infos.get(0).getFormattedMessage())
                    .contains("fromTmdb=1")
                    .contains("filteredLowVotes=0");
            assertThat(appender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .filter(event -> event.getFormattedMessage().contains("Recommendation hydrate")))
                    .isEmpty();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }

    /**
     * Register one successful movie display response for the given id.
     */
    private static void stubMovie(MockRestServiceServer server, int id, String title) {
        server.expect(requestTo("https://api.themoviedb.org/3/movie/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {"id":%d,"title":"%s","poster_path":"/p.jpg","release_date":"1990-01-01","overview":"o"}
                        """.formatted(id, title),
                        MediaType.APPLICATION_JSON));
    }
}
