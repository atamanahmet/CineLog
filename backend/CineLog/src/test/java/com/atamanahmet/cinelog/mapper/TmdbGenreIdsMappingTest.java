package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.core.io.ClassPathResource;

import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTest
class TmdbGenreIdsMappingTest {

    private static final List<Integer> ACTION_ADVENTURE = List.of(28, 12);

    @Autowired
    private ObjectMapper objectMapper;

    private final MovieMapper movieMapper = new MovieMapperImpl(new MediaMappingHelper());
    private final TvShowMapper tvShowMapper = new TvShowMapperImpl(new MediaMappingHelper());

    /**
     * By-id movie JSON with genres objects and no genre_ids maps to [28, 12].
     */
    @Test
    void movieByIdGenresMapToGenreIds() throws Exception {
        TmdbMovieResponse response = objectMapper.readValue(
                new ClassPathResource("tmdb/movie-by-id.json").getInputStream(), TmdbMovieResponse.class);
        MovieDto dto = movieMapper.toDto(response);
        Movie entity = movieMapper.toEntity(response);
        assertEquals(ACTION_ADVENTURE, dto.genreIds());
        assertEquals(ACTION_ADVENTURE, entity.getGenreIds());
    }

    /**
     * By-id TV JSON with genres objects and no genre_ids maps to [28, 12].
     */
    @Test
    void tvByIdGenresMapToGenreIds() throws Exception {
        TmdbTvShowResponse response = objectMapper.readValue(
                new ClassPathResource("tmdb/tv-by-id.json").getInputStream(), TmdbTvShowResponse.class);
        TvShowDto dto = tvShowMapper.toDto(response);
        TvShow entity = tvShowMapper.toEntity(response);
        assertEquals(ACTION_ADVENTURE, dto.genreIds());
        assertEquals(ACTION_ADVENTURE, entity.getGenreIds());
    }

    /**
     * List-shaped movie JSON with genre_ids still maps those ids.
     */
    @Test
    void movieListGenreIdsStillWin() throws Exception {
        TmdbMovieResponse response = objectMapper.readValue(
                """
                {"id":42,"title":"Heat","genre_ids":[28,12],"genres":[{"id":18,"name":"Drama"}]}
                """,
                TmdbMovieResponse.class);
        assertEquals(ACTION_ADVENTURE, movieMapper.toDto(response).genreIds());
        assertEquals(ACTION_ADVENTURE, movieMapper.toEntity(response).getGenreIds());
    }

    /**
     * List-shaped TV JSON with genre_ids still maps those ids.
     */
    @Test
    void tvListGenreIdsStillWin() throws Exception {
        TmdbTvShowResponse response = objectMapper.readValue(
                """
                {"id":1396,"name":"Breaking Bad","genre_ids":[28,12],"genres":[{"id":18,"name":"Drama"}]}
                """,
                TmdbTvShowResponse.class);
        assertEquals(ACTION_ADVENTURE, tvShowMapper.toDto(response).genreIds());
        assertEquals(ACTION_ADVENTURE, tvShowMapper.toEntity(response).getGenreIds());
    }

    /**
     * Movie JSON with neither genre_ids nor genres maps to an empty list, not null.
     */
    @Test
    void movieWithNeitherGenreFieldGivesEmptyList() throws Exception {
        TmdbMovieResponse response = objectMapper.readValue("{\"id\":1,\"title\":\"Heat\"}", TmdbMovieResponse.class);
        List<Integer> dtoIds = movieMapper.toDto(response).genreIds();
        List<Integer> entityIds = movieMapper.toEntity(response).getGenreIds();
        assertNotNull(dtoIds);
        assertNotNull(entityIds);
        assertEquals(List.of(), dtoIds);
        assertEquals(List.of(), entityIds);
    }

    /**
     * TV JSON with neither genre_ids nor genres maps to an empty list, not null.
     */
    @Test
    void tvWithNeitherGenreFieldGivesEmptyList() throws Exception {
        TmdbTvShowResponse response = objectMapper.readValue("{\"id\":1,\"name\":\"Lost\"}", TmdbTvShowResponse.class);
        List<Integer> dtoIds = tvShowMapper.toDto(response).genreIds();
        List<Integer> entityIds = tvShowMapper.toEntity(response).getGenreIds();
        assertNotNull(entityIds);
        assertNotNull(dtoIds);
        assertEquals(List.of(), dtoIds);
        assertEquals(List.of(), entityIds);
    }
}
