package com.atamanahmet.cinelog.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.ContentPolicyProperties;
import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.SearchPageResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.repository.MovieRepository;
import com.atamanahmet.cinelog.fetcher.DiscoverFetcher;
import com.atamanahmet.cinelog.service.DetailsBundle;
import com.atamanahmet.cinelog.service.DetailsCacheService;
import com.atamanahmet.cinelog.service.DiscoverCacheService;
import com.atamanahmet.cinelog.service.MovieService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;
    private final MovieMapper movieMapper;
    private final DiscoverCacheService discoverCacheService;
    private final DetailsCacheService detailsCacheService;
    private final DiscoverFetcher discoverFetcher;
    private final ContentPolicyProperties contentPolicy;

    /**
     * Fetch TMDB discover results as DTOs, from cache when possible.
     */
    @Override
    public List<MovieDto> discoverMovies(DiscoverRequest params) {
        DiscoverRequest filled = params.withDefaults(TmdbMediaType.MOVIE)
                .restrictAdult(contentPolicy.adultEnabled());
        if (filled.page() > DiscoverRequest.TMDB_MAX_PAGE) {
            return List.of();
        }
        List<MovieDto> cached = discoverCacheService.getCachedMovies(filled);
        if (cached != null) {
            return cached;
        }
        return movieMapper.toDtos(discoverFetcher.fetch(filled));
    }

    /**
     * Fetch one TMDB search page as DTOs, keeping pagination metadata.
     */
    @Override
    public SearchPageResponse<MovieDto> searchMovies(String query, int page, Integer minVotes) {
        TmdbPagedResponse<TmdbMovieResponse> tmdbPage = tmdbClient.searchMovies(query, page);
        if (minVotes != null && tmdbPage != null && tmdbPage.getResults() != null) {
            int min = minVotes;
            tmdbPage.setResults(tmdbPage.getResults().stream()
                    .filter(item -> (item.getVoteCount() == null ? 0 : item.getVoteCount()) >= min)
                    .toList());
        }
        List<MovieDto> results = movieMapper.toDtos(movieMapper.fromPage(tmdbPage));
        return new SearchPageResponse<>(tmdbPage.getPage(), results, tmdbPage.getTotalPages(),
                tmdbPage.getTotalResults());
    }

    /**
     * Fetch one movie from TMDB and map it. Does not write to the database.
     */
    @Override
    public MovieDto getDetailsFromTmdb(Integer id) {
        return detailsCacheService.getMovieDetails(id);
    }

    /**
     * Return the cached YouTube trailer URL for this movie, if any.
     */
    @Override
    public Optional<String> getTrailer(Integer id) {
        DetailsBundle<MovieDto> bundle = detailsCacheService.getMovieBundle(id);
        if (bundle == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bundle.trailerUrl());
    }

    /**
     * Return the cached top cast for this movie.
     */
    @Override
    public List<CastMember> getTopCast(Integer id) {
        DetailsBundle<MovieDto> bundle = detailsCacheService.getMovieBundle(id);
        if (bundle == null) {
            return List.of();
        }
        return bundle.topCast();
    }

    @Override
    @Transactional
    public void saveMovie(Movie movie) {
        movieRepository.save(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public Movie findMovieById(Integer id) {
        return movieRepository.findById(id).orElse(null);
    }

    /**
     * Return local movie, or fetch from TMDB, save, and return it.
     */
    @Override
    @Transactional
    public Movie findOrFetchMovie(Integer id) {
        Movie existing = findMovieById(id);
        if (existing != null) {
            return existing;
        }
        TmdbMovieResponse response = tmdbClient.getMovieById(id);
        if (response == null) {
            return null;
        }
        Movie movie = movieMapper.toEntity(response);
        try {
            saveMovie(movie);
            return movie;
        } catch (DataIntegrityViolationException e) {
            return findMovieById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movie> getMoviesFromIdSet(Set<Integer> idSet) {
        return movieRepository.findAllById(idSet);
    }
}
