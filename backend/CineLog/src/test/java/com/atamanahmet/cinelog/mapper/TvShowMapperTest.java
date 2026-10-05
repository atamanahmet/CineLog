package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class TvShowMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TvShowMapper mapper;

    @BeforeEach
    void setUp() {
        MediaMappingHelper mediaMappingHelper = new MediaMappingHelper();
        mapper = new TvShowMapperImpl(mediaMappingHelper);
    }

    @Test
    void posterPathGetsCdnPrefix() {
        TmdbTvShowResponse response = new TmdbTvShowResponse();
        response.setPosterPath("/xyz.jpg");
        assertEquals("/xyz.jpg", mapper.toEntity(response).getPosterPath());
    }

    @Test
    void nullPosterPathStaysNull() {
        TmdbTvShowResponse response = new TmdbTvShowResponse();
        response.setPosterPath(null);
        assertNull(mapper.toEntity(response).getPosterPath());
    }

    @Test
    void mapsFullTvDetailsFixture() throws Exception {
        TmdbTvShowResponse response = readFixture("tmdb/tv-details-full.json");
        TvShowDto dto = mapper.toDto(response);

        assertEquals("Ended", dto.status());
        assertEquals(8, dto.numberOfSeasons());
        assertEquals(73, dto.numberOfEpisodes());
        assertNull(dto.episodeRunTime());
        assertEquals(LocalDate.of(2011, 4, 17), dto.firstAirDate());
        assertEquals(LocalDate.of(2019, 5, 19), dto.lastAirDate());
        assertEquals(LocalDate.of(2011, 4, 17), dto.releaseDate());
        assertEquals(1, dto.networks().size());
        assertEquals("HBO", dto.networks().get(0));
        assertEquals(1, dto.createdBy().size());
        assertEquals(9813, dto.createdBy().get(0).id());
        assertEquals("David Benioff", dto.createdBy().get(0).name());
        assertNull(dto.nextEpisodeToAir());
        assertEquals(1, dto.seasons().size());
        assertEquals(1, dto.seasons().get(0).seasonNumber());
        assertEquals("Season 1", dto.seasons().get(0).name());
        assertEquals(10, dto.seasons().get(0).episodeCount());
    }

    @Test
    void mapsNextEpisodeAndRuntime() throws Exception {
        TmdbTvShowResponse response = readFixture("tmdb/tv-details-next-episode.json");
        TvShowDto dto = mapper.toDto(response);

        assertEquals(45, dto.episodeRunTime());
        assertEquals(LocalDate.of(2026, 3, 15), dto.nextEpisodeToAir().airDate());
        assertEquals(2, dto.nextEpisodeToAir().seasonNumber());
        assertEquals(5, dto.nextEpisodeToAir().episodeNumber());
        assertEquals("Next One", dto.nextEpisodeToAir().name());
    }

    private TmdbTvShowResponse readFixture(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            return objectMapper.readValue(in, TmdbTvShowResponse.class);
        }
    }
}
