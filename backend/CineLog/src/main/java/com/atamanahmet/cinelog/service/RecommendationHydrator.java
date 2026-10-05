package com.atamanahmet.cinelog.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.client.tmdb.TmdbRateLimitExceededException;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.RecommendationCacheProperties;
import com.atamanahmet.cinelog.config.TmdbClientConfig;
import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.CatalogCacheId;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecommendationHydrator {

    private final CatalogCacheRepository catalogCacheRepository;
    private final TmdbClient tmdbClient;
    private final Cache<MediaKey, MediaDisplay> displayCache;
    private final ExecutorService hydrateExecutor;
    private final RecommendationCacheProperties properties;
    private final RecommendationQualityFilter qualityFilter;

    public RecommendationHydrator(
            CatalogCacheRepository catalogCacheRepository,
            @Qualifier(TmdbClientConfig.TMDB_HYDRATE_CLIENT) TmdbClient tmdbClient,
            @Qualifier(CacheConfig.RECOMMENDATION_DISPLAY_CACHE) Cache<MediaKey, MediaDisplay> displayCache,
            @Qualifier(CacheConfig.RECOMMENDATION_HYDRATE_EXECUTOR) ExecutorService hydrateExecutor,
            RecommendationCacheProperties properties,
            RecommendationQualityFilter qualityFilter) {
        this.catalogCacheRepository = catalogCacheRepository;
        this.tmdbClient = tmdbClient;
        this.displayCache = displayCache;
        this.hydrateExecutor = hydrateExecutor;
        this.properties = properties;
        this.qualityFilter = qualityFilter;
    }

    /**
     * Hydrate keys from memory cache and catalog_cache only. Never calls TMDB.
     */
    public List<RecommendationItemDTO> hydrateCacheOnly(List<MediaKey> keys) {
        return assemble(keys, Map.of(), false);
    }

    /**
     * Hydrate keys from memory cache, catalog_cache, then TMDB. Keeps engine order and scores.
     */
    public List<RecommendationItemDTO> hydrate(List<MediaKey> keys, Map<MediaKey, Double> scoresByKey) {
        return assemble(keys, scoresByKey == null ? Map.of() : scoresByKey, true);
    }

    /**
     * Resolve display rows tier by tier, build DTOs, then quality-filter the result.
     */
    private List<RecommendationItemDTO> assemble(
            List<MediaKey> keys,
            Map<MediaKey, Double> scoresByKey,
            boolean allowTmdb) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        int fromMemory = 0;
        int fromCatalog = 0;
        Map<MediaKey, MediaDisplay> resolved = new HashMap<>();
        List<MediaKey> missing = new ArrayList<>();
        for (MediaKey key : keys) {
            MediaDisplay cached = displayCache.getIfPresent(key);
            if (cached != null) {
                resolved.put(key, cached);
                fromMemory++;
            } else {
                missing.add(key);
            }
        }

        if (!missing.isEmpty()) {
            Map<TmdbMediaType, List<Integer>> idsByType = missing.stream()
                    .collect(Collectors.groupingBy(
                            MediaKey::mediaType,
                            Collectors.mapping(MediaKey::tmdbId, Collectors.toList())));
            Map<CatalogCacheId, CatalogCache> cacheById = new HashMap<>();
            for (Map.Entry<TmdbMediaType, List<Integer>> entry : idsByType.entrySet()) {
                List<Integer> tmdbIds = entry.getValue();
                if (tmdbIds == null || tmdbIds.isEmpty()) {
                    continue;
                }
                for (CatalogCache cache : catalogCacheRepository.findByMediaTypeAndTmdbIdIn(
                        entry.getKey(), tmdbIds)) {
                    cacheById.put(new CatalogCacheId(cache.getTmdbId(), cache.getMediaType()), cache);
                }
            }
            List<MediaKey> stillMissing = new ArrayList<>();
            for (MediaKey key : missing) {
                CatalogCache cache = cacheById.get(new CatalogCacheId(key.tmdbId(), key.mediaType()));
                if (cache != null) {
                    resolved.put(key, fromCatalog(cache));
                    fromCatalog++;
                } else {
                    stillMissing.add(key);
                }
            }
            missing = stillMissing;
        }

        boolean tmdbPathRan = allowTmdb && !missing.isEmpty();
        TmdbFetchStats tmdbStats = TmdbFetchStats.EMPTY;
        if (tmdbPathRan) {
            tmdbStats = fetchFromTmdb(missing, resolved);
        }

        List<RecommendationItemDTO> items = new ArrayList<>();
        for (MediaKey key : keys) {
            MediaDisplay display = resolved.get(key);
            if (display == null) {
                continue;
            }
            items.add(RecommendationItemDTO.fromDisplay(display, scoresByKey.get(key)));
        }

        RecommendationQualityFilter.Result filtered = qualityFilter.apply(items);
        logSummary(keys.size(), fromMemory, fromCatalog, tmdbStats, tmdbPathRan, filtered.dropped());
        return filtered.kept();
    }

    /**
     * Log hydrate summary when TMDB ran or low-vote rows were dropped.
     */
    private void logSummary(
            int requested,
            int fromMemory,
            int fromCatalog,
            TmdbFetchStats tmdbStats,
            boolean tmdbPathRan,
            int filteredLowVotes) {
        if (!tmdbPathRan && filteredLowVotes <= 0) {
            return;
        }
        boolean realSkips = tmdbStats.skippedRateLimit() > 0
                || tmdbStats.skippedTimeout() > 0
                || tmdbStats.skippedUpstream429() > 0
                || tmdbStats.skippedNotFound() > 0
                || tmdbStats.skippedOther() > 0;
        String message =
                "Recommendation hydrate requested={} fromMemory={} fromCatalog={} fromTmdb={} "
                        + "skippedRateLimit={} skippedTimeout={} skippedUpstream429={} "
                        + "skippedNotFound={} skippedOther={} filteredLowVotes={}";
        Object[] args = {
                requested,
                fromMemory,
                fromCatalog,
                tmdbStats.fromTmdb(),
                tmdbStats.skippedRateLimit(),
                tmdbStats.skippedTimeout(),
                tmdbStats.skippedUpstream429(),
                tmdbStats.skippedNotFound(),
                tmdbStats.skippedOther(),
                filteredLowVotes
        };
        if (realSkips) {
            log.warn(message, args);
        } else {
            log.info(message, args);
        }
    }

    /**
     * Fetch missing keys from TMDB in parallel and put successes into memory cache only.
     */
    private TmdbFetchStats fetchFromTmdb(List<MediaKey> missing, Map<MediaKey, MediaDisplay> resolved) {
        AtomicInteger fromTmdb = new AtomicInteger();
        AtomicInteger skippedRateLimit = new AtomicInteger();
        AtomicInteger skippedTimeout = new AtomicInteger();
        AtomicInteger skippedUpstream429 = new AtomicInteger();
        AtomicInteger skippedNotFound = new AtomicInteger();
        AtomicInteger skippedOther = new AtomicInteger();

        Instant deadline = Instant.now().plusSeconds(properties.getHydrateTimeoutSeconds());
        AtomicBoolean timedOut = new AtomicBoolean(false);
        CountDownLatch finished = new CountDownLatch(missing.size());
        List<Future<?>> futures = new ArrayList<>(missing.size());
        for (MediaKey key : missing) {
            futures.add(hydrateExecutor.submit(() -> {
                try {
                    if (!Instant.now().isBefore(deadline)) {
                        skippedTimeout.incrementAndGet();
                        return;
                    }
                    MediaDisplay display = fetchOne(key, deadline);
                    if (display == null) {
                        return;
                    }
                    displayCache.put(key, display);
                    synchronized (resolved) {
                        resolved.put(key, display);
                    }
                    fromTmdb.incrementAndGet();
                } catch (TmdbRateLimitExceededException e) {
                    skippedRateLimit.incrementAndGet();
                } catch (TmdbClientException e) {
                    if (e.getUpstreamStatus() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                        skippedUpstream429.incrementAndGet();
                    } else if (e.getUpstreamStatus() == 404) {
                        skippedNotFound.incrementAndGet();
                    } else if (isWaitInterrupted(e)) {
                        if (!timedOut.get()) {
                            skippedTimeout.incrementAndGet();
                        }
                    } else {
                        skippedOther.incrementAndGet();
                        log.warn("TMDB display failed key={} status={}", key, e.getUpstreamStatus());
                    }
                } catch (RuntimeException e) {
                    skippedOther.incrementAndGet();
                    log.warn("TMDB display failed key={}", key, e);
                } finally {
                    finished.countDown();
                }
            }));
        }
        awaitOrCancel(futures, deadline, timedOut, skippedTimeout);
        drainTasks(finished);
        return new TmdbFetchStats(
                fromTmdb.get(),
                skippedRateLimit.get(),
                skippedTimeout.get(),
                skippedUpstream429.get(),
                skippedNotFound.get(),
                skippedOther.get());
    }

    /**
     * Wait until the deadline, then interrupt any unfinished ExecutorService tasks.
     */
    private static void awaitOrCancel(
            List<Future<?>> futures,
            Instant deadline,
            AtomicBoolean timedOut,
            AtomicInteger skippedTimeout) {
        for (Future<?> future : futures) {
            long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
            if (remainingMillis <= 0L) {
                break;
            }
            try {
                future.get(remainingMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                timedOut.set(true);
                cancelUnfinished(futures, skippedTimeout);
                return;
            } catch (ExecutionException e) {
                // per-task failures are counted inside the task
            }
        }
        if (futures.stream().anyMatch(future -> !future.isDone())) {
            timedOut.set(true);
            cancelUnfinished(futures, skippedTimeout);
        }
    }

    /**
     * Cancel unfinished lookups so waiting permit acquires are interrupted.
     */
    private static void cancelUnfinished(List<Future<?>> futures, AtomicInteger skippedTimeout) {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                skippedTimeout.incrementAndGet();
                future.cancel(true);
            }
        }
    }

    /**
     * Block until every hydrate task has stopped so no further permits are taken.
     */
    private static void drainTasks(CountDownLatch finished) {
        try {
            finished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * True when the client aborted because the hydrate thread was interrupted while waiting.
     */
    private static boolean isWaitInterrupted(TmdbClientException e) {
        Throwable cause = e.getCause();
        return cause instanceof InterruptedException;
    }

    /**
     * Call the matching light TMDB display method for one key using the request deadline.
     */
    private MediaDisplay fetchOne(MediaKey key, Instant deadline) {
        return switch (key.mediaType()) {
            case MOVIE -> tmdbClient.getMovieDisplayById(key.tmdbId(), deadline);
            case TV -> tmdbClient.getTvDisplayById(key.tmdbId(), deadline);
        };
    }

    /**
     * Map a catalog_cache row into MediaDisplay without caching it.
     */
    private static MediaDisplay fromCatalog(CatalogCache cache) {
        return new MediaDisplay(
                cache.getTmdbId(),
                cache.getMediaType(),
                cache.getTitle(),
                cache.getPosterPath(),
                cache.getReleaseDate(),
                cache.getOverview(),
                null,
                null,
                List.of());
    }

    private record TmdbFetchStats(
            int fromTmdb,
            int skippedRateLimit,
            int skippedTimeout,
            int skippedUpstream429,
            int skippedNotFound,
            int skippedOther) {

        private static final TmdbFetchStats EMPTY = new TmdbFetchStats(0, 0, 0, 0, 0, 0);
    }
}
