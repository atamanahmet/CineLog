package com.atamanahmet.cinelog.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTest
class MovieDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Movie DTO JSON uses camelCase keys and keeps the raw poster path.
     */
    @Test
    void serializesCamelCaseKeysAndRawPosterPath() throws Exception {
        MovieDto dto = new MovieDto(
                550,
                false,
                "/back.jpg",
                List.of(18, 80),
                "en",
                "Fight Club",
                "overview",
                1.0,
                "/abc.jpg",
                LocalDate.of(2026, 7, 8),
                "Fight Club",
                false,
                8.4,
                100);
        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertTrue(node.has("id"));
        assertTrue(node.has("adult"));
        assertTrue(node.has("backdropPath"));
        assertTrue(node.has("genreIds"));
        assertTrue(node.has("originalLanguage"));
        assertTrue(node.has("originalTitle"));
        assertTrue(node.has("overview"));
        assertTrue(node.has("popularity"));
        assertTrue(node.has("posterPath"));
        assertTrue(node.has("releaseDate"));
        assertTrue(node.has("title"));
        assertTrue(node.has("video"));
        assertTrue(node.has("voteAverage"));
        assertTrue(node.has("voteCount"));
        assertFalse(node.has("poster_path"));
        assertFalse(node.has("backdrop_path"));
        assertFalse(node.has("release_date"));
        assertFalse(node.has("vote_average"));
        assertFalse(node.has("vote_count"));
        assertFalse(node.has("genre_ids"));
        assertEquals("/abc.jpg", node.get("posterPath").asText());
        assertTrue(node.get("releaseDate").isTextual());
        assertFalse(node.get("releaseDate").isArray());
        assertEquals("2026-07-08", node.get("releaseDate").asText());
    }

    /**
     * Null genreIds becomes an empty JSON array on write.
     */
    @Test
    void serializesNullGenreIdsAsEmptyArray() throws Exception {
        MovieDto dto = new MovieDto(
                550,
                false,
                "/back.jpg",
                null,
                "en",
                "Fight Club",
                "overview",
                1.0,
                "/abc.jpg",
                LocalDate.of(2026, 7, 8),
                "Fight Club",
                false,
                8.4,
                100);
        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertTrue(node.get("genreIds").isArray());
        assertEquals(0, node.get("genreIds").size());
    }
}
