package com.atamanahmet.cinelog.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenre;
import com.atamanahmet.cinelog.service.DetailsBundle;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties({
        DiscoverCacheProperties.class,
        DetailsCacheProperties.class,
        PersonCacheProperties.class,
        GenreCacheProperties.class,
        RecommendationCacheProperties.class
})
public class CacheConfig {

    public static final String MOVIE_DISCOVER_CACHE = "movieDiscoverCache";
    public static final String TV_DISCOVER_CACHE = "tvDiscoverCache";
    public static final String MOVIE_DETAILS_CACHE = "movieDetailsCache";
    public static final String TV_DETAILS_CACHE = "tvDetailsCache";
    public static final String PERSON_CACHE = "personCache";
    public static final String GENRE_CACHE = "genreCache";
    public static final String RECOMMENDATION_DISPLAY_CACHE = "recommendationDisplayCache";
    public static final String RECOMMENDATION_HYDRATE_EXECUTOR = "recommendationHydrateExecutor";

    /**
     * Build the movie discover cache keyed by sort option.
     */
    @Bean(name = MOVIE_DISCOVER_CACHE)
    public Cache<SortOption, List<MovieDto>> movieDiscoverCache(DiscoverCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(SortOption.values().length)
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the TV discover cache keyed by sort option.
     */
    @Bean(name = TV_DISCOVER_CACHE)
    public Cache<SortOption, List<TvShowDto>> tvDiscoverCache(DiscoverCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(SortOption.values().length)
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the movie details cache keyed by TMDB id.
     */
    @Bean(name = MOVIE_DETAILS_CACHE)
    public Cache<Integer, DetailsBundle<MovieDto>> movieDetailsCache(DetailsCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the TV details cache keyed by TMDB id.
     */
    @Bean(name = TV_DETAILS_CACHE)
    public Cache<Integer, DetailsBundle<TvShowDto>> tvDetailsCache(DetailsCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the person details cache keyed by TMDB id.
     */
    @Bean(name = PERSON_CACHE)
    public Cache<Integer, PersonDetailDto> personCache(PersonCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the genre list cache keyed by media type.
     */
    @Bean(name = GENRE_CACHE)
    public Cache<TmdbMediaType, List<TmdbGenre>> genreCache(GenreCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(TmdbMediaType.values().length)
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Build the recommendation display cache keyed by MediaKey.
     */
    @Bean(name = RECOMMENDATION_DISPLAY_CACHE)
    public Cache<MediaKey, MediaDisplay> recommendationDisplayCache(RecommendationCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlHours(), TimeUnit.HOURS)
                .build();
    }

    /**
     * Bounded pool for parallel TMDB display fetches during recommendation hydrate.
     */
    @Bean(name = RECOMMENDATION_HYDRATE_EXECUTOR, destroyMethod = "shutdown")
    public ExecutorService recommendationHydrateExecutor(RecommendationCacheProperties properties) {
        int poolSize = Math.max(1, properties.getHydratePoolSize());
        AtomicInteger next = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("rec-hydrate-" + next.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, factory);
    }
}
