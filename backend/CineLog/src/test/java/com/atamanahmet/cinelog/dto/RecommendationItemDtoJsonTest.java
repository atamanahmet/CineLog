package com.atamanahmet.cinelog.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTest
class RecommendationItemDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Recommendation item JSON exposes genre_ids as an array.
     */
    @Test
    void serializesGenreIdsArray() throws Exception {
        RecommendationItemDTO dto = new RecommendationItemDTO(
                99,
                "Heat",
                "Heat",
                "/heat.jpg",
                LocalDate.of(1995, 12, 15),
                "Crime drama",
                7.9,
                null,
                0.91,
                TmdbMediaType.MOVIE,
                List.of(28, 80));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertTrue(json.has("genre_ids"));
        assertTrue(json.get("genre_ids").isArray());
        assertEquals(28, json.get("genre_ids").get(0).asInt());
        assertEquals(80, json.get("genre_ids").get(1).asInt());
    }

    /**
     * Recommendation item JSON exposes vote_count when present.
     */
    @Test
    void serializesVoteCount() throws Exception {
        RecommendationItemDTO dto = new RecommendationItemDTO(
                99,
                "Heat",
                "Heat",
                "/heat.jpg",
                LocalDate.of(1995, 12, 15),
                "Crime drama",
                7.9,
                3210,
                0.91,
                TmdbMediaType.MOVIE,
                List.of());

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertEquals(3210, json.get("vote_count").asInt());
    }
}
