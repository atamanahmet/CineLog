package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;

class MovieMapperTest {

    private final MovieMapper mapper = new MovieMapperImpl(new MediaMappingHelper());

    @Test
    void posterPathGetsCdnPrefix() {
        TmdbMovieResponse response = new TmdbMovieResponse();
        response.setPosterPath("/abc.jpg");
        Movie movie = mapper.toEntity(response);
        assertEquals("/abc.jpg", movie.getPosterPath());
    }

    @Test
    void nullPosterPathStaysNull() {
        TmdbMovieResponse response = new TmdbMovieResponse();
        response.setPosterPath(null);
        Movie movie = mapper.toEntity(response);
        assertNull(movie.getPosterPath());
    }

    /**
     * Details mapping prefixes images, parses the release date, and copies id, title, and genres.
     */
    @Test
    void toDtoFromTmdbMapsDetailsFields() {
        TmdbMovieResponse response = new TmdbMovieResponse();
        response.setId(42);
        response.setTitle("Heat");
        response.setPosterPath("/p.jpg");
        response.setBackdropPath("/b.jpg");
        response.setReleaseDate("1995-12-15");
        response.setGenreIds(List.of(28, 80));
        MovieDto dto = mapper.toDto(response);
        assertEquals(42, dto.id());
        assertEquals("Heat", dto.title());
        assertEquals("/p.jpg", dto.posterPath());
        assertEquals("/b.jpg", dto.backdropPath());
        assertEquals(LocalDate.of(1995, 12, 15), dto.releaseDate());
        assertEquals(List.of(28, 80), dto.genreIds());
    }
}
