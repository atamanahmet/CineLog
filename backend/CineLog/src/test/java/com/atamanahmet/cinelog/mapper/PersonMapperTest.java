package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.dto.PersonCreditDto;
import com.atamanahmet.cinelog.dto.PersonCreditRoleDto;
import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPersonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class PersonMapperTest {

    private final PersonMapper mapper = new PersonMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsFixtureWithMergeCapAndNullDate() throws Exception {
        TmdbPersonResponse response = readFixture("tmdb/person-with-credits.json");
        PersonDetailDto dto = mapper.toDetail(response);

        assertEquals(287, dto.person().id());
        assertEquals("Brad Pitt", dto.person().name());
        assertEquals(5, dto.person().alsoKnownAs().size());
        assertEquals(List.of("A", "B", "C", "D", "E"), dto.person().alsoKnownAs());

        assertEquals(3, dto.credits().size());

        PersonCreditDto fightClub = dto.credits().get(0);
        assertEquals("movie", fightClub.mediaType());
        assertEquals(550, fightClub.tmdbId());
        assertEquals("Fight Club", fightClub.title());
        assertEquals(2, fightClub.roles().size());
        assertEquals(new PersonCreditRoleDto("cast", "Tyler Durden", null, null), fightClub.roles().get(0));
        assertEquals(new PersonCreditRoleDto("crew", "Producer", "Production", null), fightClub.roles().get(1));

        PersonCreditDto got = dto.credits().get(1);
        assertEquals("tv", got.mediaType());
        assertEquals(1399, got.tmdbId());
        assertNull(got.date());
        assertNull(got.posterPath());
        assertEquals(1, got.roles().size());
        assertEquals("cast", got.roles().get(0).kind());
        assertEquals(1, got.roles().get(0).episodeCount());

        PersonCreditDto planB = dto.credits().get(2);
        assertEquals(700, planB.tmdbId());
        assertEquals(1, planB.roles().size());
        assertEquals("crew", planB.roles().get(0).kind());

        assertTrue(dto.credits().stream().noneMatch(c -> c.tmdbId() == 999));
    }

    private TmdbPersonResponse readFixture(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            return objectMapper.readValue(in, TmdbPersonResponse.class);
        }
    }
}
