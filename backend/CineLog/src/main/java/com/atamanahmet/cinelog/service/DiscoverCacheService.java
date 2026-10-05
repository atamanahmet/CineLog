package com.atamanahmet.cinelog.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.DiscoverCacheProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.fetcher.DiscoverFetcher;
import com.atamanahmet.cinelog.fetcher.TvDiscoverFetcher;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscoverCacheService {

    static final boolean DEFAULT_ADULT = false;
    static final String DEFAULT_RELEASE_WINDOW = "";
    static final int[] DEFAULT_YEAR_RANGE = {
            DiscoverRequest.DEFAULT_YEAR_MIN, DiscoverRequest.DEFAULT_YEAR_MAX };
    static final int[] DEFAULT_RATING_RANGE = {
            DiscoverRequest.MIN_RATING, DiscoverRequest.MAX_RATING };
    static final String DEFAULT_LANGUAGES = "en";

    private final Cache<SortOption, List<MovieDto>> movieDiscoverCache;
    private final Cache<SortOption, List<TvShowDto>> tvDiscoverCache;
    private final DiscoverFetcher discoverFetcher;
    private final TvDiscoverFetcher tvDiscoverFetcher;
    private final MovieMapper movieMapper;
    private final TvShowMapper tvShowMapper;
    private final DiscoverCacheProperties properties;

    public DiscoverCacheService(
            @Qualifier(CacheConfig.MOVIE_DISCOVER_CACHE) Cache<SortOption, List<MovieDto>> movieDiscoverCache,
            @Qualifier(CacheConfig.TV_DISCOVER_CACHE) Cache<SortOption, List<TvShowDto>> tvDiscoverCache,
            DiscoverFetcher discoverFetcher,
            TvDiscoverFetcher tvDiscoverFetcher,
            MovieMapper movieMapper,
            TvShowMapper tvShowMapper,
            DiscoverCacheProperties properties) {
        this.movieDiscoverCache = movieDiscoverCache;
        this.tvDiscoverCache = tvDiscoverCache;
        this.discoverFetcher = discoverFetcher;
        this.tvDiscoverFetcher = tvDiscoverFetcher;
        this.movieMapper = movieMapper;
        this.tvShowMapper = tvShowMapper;
        this.properties = properties;
    }

    /**
     * Return one page from the movie cache when only default filters plus sort are set.
     */
    public List<MovieDto> getCachedMovies(DiscoverRequest request) {
        SortOption sortOption = warmedCacheKey(request, TmdbMediaType.MOVIE);
        if (sortOption == null) {
            return null;
        }
        List<MovieDto> cached = movieDiscoverCache.getIfPresent(sortOption);
        if (cached == null || cached.isEmpty()) {
            return null;
        }
        return slicePage(cached, request.withDefaults(TmdbMediaType.MOVIE).page(), DiscoverFetcher.TMDB_PAGE_SIZE);
    }

    /**
     * Return one page from the TV cache when only default filters plus sort are set.
     */
    public List<TvShowDto> getCachedTvShows(DiscoverRequest request) {
        SortOption sortOption = warmedCacheKey(request, TmdbMediaType.TV);
        if (sortOption == null) {
            return null;
        }
        List<TvShowDto> cached = tvDiscoverCache.getIfPresent(sortOption);
        if (cached == null || cached.isEmpty()) {
            return null;
        }
        return slicePage(cached, request.withDefaults(TmdbMediaType.TV).page(), TvDiscoverFetcher.TMDB_PAGE_SIZE);
    }

    /**
     * SortOption used as the discover cache key when filters match a warmed entry.
     */
    public SortOption warmedCacheKey(DiscoverRequest request, TmdbMediaType mediaType) {
        if (request == null) {
            return null;
        }
        DiscoverRequest filled = request.withDefaults(mediaType);
        if (mediaType == TmdbMediaType.MOVIE && !isDefaultMovieFilters(filled)) {
            return null;
        }
        if (mediaType == TmdbMediaType.TV && !isDefaultTvFilters(filled)) {
            return null;
        }
        return filled.sort();
    }

    /**
     * Warm every media type and sort option into the discover caches.
     */
    @Scheduled(initialDelay = 0, fixedRateString = "${discover.cache.refresh-hours}", timeUnit = TimeUnit.HOURS)
    public void warmAll() {
        log.info("Warming discover cache");
        for (SortOption sortOption : SortOption.values()) {
            warmMovie(sortOption);
            warmTv(sortOption);
        }
        log.info("Discover cache warm finished");
    }

    /**
     * Warm one movie sort; keep the previous entry if this attempt fails.
     */
    private void warmMovie(SortOption sortOption) {
        try {
            List<MovieDto> items = movieMapper.toDtos(discoverFetcher.fetchUpTo(
                    DiscoverRequest.forWarm(sortOption, TmdbMediaType.MOVIE), properties.getItemsPerSort()));
            if (!items.isEmpty()) {
                movieDiscoverCache.put(sortOption, items);
                log.info("Warmed movie discover cache for sort {} with {} items", sortOption, items.size());
            } else {
                log.warn("Empty movie discover result for sort {}; previous entry kept", sortOption);
            }
        } catch (Exception e) {
            log.error("Failed to warm movie discover cache for sort {}", sortOption, e);
        }
    }

    /**
     * Warm one TV sort; keep the previous entry if this attempt fails.
     */
    private void warmTv(SortOption sortOption) {
        try {
            List<TvShowDto> items = tvShowMapper.toDtos(tvDiscoverFetcher.fetchUpTo(
                    DiscoverRequest.forWarm(sortOption, TmdbMediaType.TV), properties.getItemsPerSort()));
            if (!items.isEmpty()) {
                tvDiscoverCache.put(sortOption, items);
                log.info("Warmed TV discover cache for sort {} with {} items", sortOption, items.size());
            } else {
                log.warn("Empty TV discover result for sort {}; previous entry kept", sortOption);
            }
        } catch (Exception e) {
            log.error("Failed to warm TV discover cache for sort {}", sortOption, e);
        }
    }

    /**
     * Slice one TMDB-sized page from the cached list, or null if out of range.
     */
    private <T> List<T> slicePage(List<T> cached, Integer pageParam, int pageSize) {
        int pageNum = pageParam == null ? 1 : pageParam;
        if (pageNum < 1) {
            return null;
        }
        int from = (pageNum - 1) * pageSize;
        if (from >= cached.size()) {
            return null;
        }
        int to = Math.min(from + pageSize, cached.size());
        return cached.subList(from, to);
    }

    private boolean isDefaultMovieFilters(DiscoverRequest params) {
        return params != null
                && Boolean.valueOf(DEFAULT_ADULT).equals(params.adult())
                && isBlankOrDefault(params.releaseWindow(), DEFAULT_RELEASE_WINDOW)
                && Integer.valueOf(DiscoverRequest.DEFAULT_MIN_VOTES).equals(params.voteCount())
                && (params.genreIdList() == null || params.genreIdList().isEmpty())
                && (params.withoutGenres() == null || params.withoutGenres().isEmpty())
                && Arrays.equals(params.yearRange(), DEFAULT_YEAR_RANGE)
                && Arrays.equals(params.ratingRange(), DEFAULT_RATING_RANGE)
                && DEFAULT_LANGUAGES.equals(params.languages())
                && !params.hasRuntimeFilter();
    }

    private boolean isDefaultTvFilters(DiscoverRequest params) {
        return params != null
                && Boolean.valueOf(DEFAULT_ADULT).equals(params.adult())
                && isBlankOrDefault(params.releaseWindow(), DEFAULT_RELEASE_WINDOW)
                && Integer.valueOf(DiscoverRequest.DEFAULT_MIN_VOTES).equals(params.voteCount())
                && (params.genreIdList() == null || params.genreIdList().isEmpty())
                && (params.withoutGenres() == null || params.withoutGenres().isEmpty())
                && Arrays.equals(params.yearRange(), DEFAULT_YEAR_RANGE)
                && Arrays.equals(params.ratingRange(), DEFAULT_RATING_RANGE)
                && (params.languages() == null || params.languages().isBlank())
                && !params.hasRuntimeFilter();
    }

    private boolean isBlankOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue.isEmpty();
        }
        return defaultValue.equals(value);
    }
}
