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
class TvShowDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * TV DTO JSON uses camelCase keys title, originalTitle, and releaseDate.
     */
    @Test
    void serializesMovieKeyedTitleAndDateFields() throws Exception {
        TvShowDto dto = new TvShowDto(
                1396,
                false,
                null,
                List.of(18),
                List.of("US"),
                "en",
                "Breaking Bad",
                "overview",
                1.0,
                null,
                LocalDate.of(2008, 1, 20),
                "Breaking Bad",
                8.8,
                100,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                List.of());
        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertEquals("Breaking Bad", node.get("title").asText());
        assertEquals("Breaking Bad", node.get("originalTitle").asText());
        assertEquals("2008-01-20", node.get("releaseDate").asText());
        assertTrue(node.has("title"));
        assertTrue(node.has("originalTitle"));
        assertTrue(node.has("releaseDate"));
        assertFalse(node.has("name"));
        assertFalse(node.has("original_name"));
        assertFalse(node.has("first_air_date"));
        assertFalse(node.has("original_title"));
        assertFalse(node.has("release_date"));
    }
}
