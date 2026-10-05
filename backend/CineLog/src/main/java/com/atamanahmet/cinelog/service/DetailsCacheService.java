package com.atamanahmet.cinelog.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbTrailerUrls;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbCast;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.github.benmanes.caffeine.cache.Cache;

@Service
public class DetailsCacheService {

    private static final int TOP_CAST_LIMIT = 16;

    private final Cache<Integer, DetailsBundle<MovieDto>> movieDetailsCache;
    private final Cache<Integer, DetailsBundle<TvShowDto>> tvDetailsCache;
    private final TmdbClient tmdbClient;
    private final MovieMapper movieMapper;
    private final TvShowMapper tvShowMapper;

    public DetailsCacheService(
            @Qualifier(CacheConfig.MOVIE_DETAILS_CACHE) Cache<Integer, DetailsBundle<MovieDto>> movieDetailsCache,
            @Qualifier(CacheConfig.TV_DETAILS_CACHE) Cache<Integer, DetailsBundle<TvShowDto>> tvDetailsCache,
            TmdbClient tmdbClient,
            MovieMapper movieMapper,
            TvShowMapper tvShowMapper) {
        this.movieDetailsCache = movieDetailsCache;
        this.tvDetailsCache = tvDetailsCache;
        this.tmdbClient = tmdbClient;
        this.movieMapper = movieMapper;
        this.tvShowMapper = tvShowMapper;
    }

    /**
     * Return movie details, top cast, and trailer from cache, or fetch once from TMDB.
     */
    public DetailsBundle<MovieDto> getMovieBundle(Integer id) {
        return movieDetailsCache.get(id, this::loadMovieBundle);
    }

    /**
     * Return TV show details, top cast, and trailer from cache, or fetch once from TMDB.
     */
    public DetailsBundle<TvShowDto> getTvShowBundle(Integer id) {
        return tvDetailsCache.get(id, this::loadTvShowBundle);
    }

    /**
     * Return movie details from cache, or fetch from TMDB and cache on miss.
     */
    public MovieDto getMovieDetails(Integer id) {
        DetailsBundle<MovieDto> bundle = getMovieBundle(id);
        return bundle == null ? null : bundle.details();
    }

    /**
     * Return TV show details from cache, or fetch from TMDB and cache on miss.
     */
    public TvShowDto getTvShowDetails(Integer id) {
        DetailsBundle<TvShowDto> bundle = getTvShowBundle(id);
        return bundle == null ? null : bundle.details();
    }

    private DetailsBundle<MovieDto> loadMovieBundle(Integer id) {
        TmdbMovieResponse response = tmdbClient.getMovieById(id);
        if (response == null) {
            return null;
        }
        return new DetailsBundle<>(movieMapper.toDto(response), topBilledCast(response.getCredits()),
                TmdbTrailerUrls.firstYoutubeTrailer(response.getVideos()).orElse(null));
    }

    private DetailsBundle<TvShowDto> loadTvShowBundle(Integer id) {
        TmdbTvShowResponse response = tmdbClient.getTvShowById(id);
        if (response == null) {
            return null;
        }
        return new DetailsBundle<>(tvShowMapper.toDto(response), topBilledCast(response.getCredits()),
                TmdbTrailerUrls.firstYoutubeTrailer(response.getVideos()).orElse(null));
    }

    /**
     * Return the cast sorted by popularity, capped at the limit.
     */
    private List<CastMember> topBilledCast(TmdbCast credits) {
        if (credits == null || credits.getCast() == null) {
            return List.of();
        }
        return credits.getCast().stream()
                .sorted(Comparator.comparingDouble(CastMember::getPopularity).reversed())
                .limit(TOP_CAST_LIMIT)
                .toList();
    }
}
