package com.atamanahmet.cinelog.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.SearchPageResponse;

public interface MovieService {

    List<MovieDto> discoverMovies(DiscoverRequest params);

    SearchPageResponse<MovieDto> searchMovies(String query, int page, Integer minVotes);

    /**
     * Live TMDB detail fetch, mapped to MovieDto. No persistence.
     */
    MovieDto getDetailsFromTmdb(Integer id);

    /**
     * Return the cached YouTube trailer URL for this movie, if any.
     */
    Optional<String> getTrailer(Integer id);

    /**
     * Return the cached top cast for this movie.
     */
    List<CastMember> getTopCast(Integer id);

    void saveMovie(Movie movie);

    Movie findMovieById(Integer id);

    /**
     * Return local movie, or fetch from TMDB, save, and return it.
     */
    Movie findOrFetchMovie(Integer id);

    List<Movie> getMoviesFromIdSet(Set<Integer> idSet);
}
