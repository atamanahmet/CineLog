package com.atamanahmet.cinelog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenre;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenreListResponse;
import com.github.benmanes.caffeine.cache.Cache;

@Service
public class GenreCacheService {

    private final Cache<TmdbMediaType, List<TmdbGenre>> genreCache;
    private final TmdbClient tmdbClient;

    public GenreCacheService(
            @Qualifier(CacheConfig.GENRE_CACHE) Cache<TmdbMediaType, List<TmdbGenre>> genreCache,
            TmdbClient tmdbClient) {
        this.genreCache = genreCache;
        this.tmdbClient = tmdbClient;
    }

    /**
     * Return genres for the media type from cache, or fetch from TMDB on miss.
     */
    public List<TmdbGenre> getGenres(TmdbMediaType mediaType) {
        return genreCache.get(mediaType, this::loadGenres);
    }

    private List<TmdbGenre> loadGenres(TmdbMediaType mediaType) {
        TmdbGenreListResponse response = switch (mediaType) {
            case MOVIE -> tmdbClient.getMovieGenres();
            case TV -> tmdbClient.getTvGenres();
        };
        if (response == null || response.genres() == null) {
            return List.of();
        }
        return List.copyOf(response.genres());
    }
}
