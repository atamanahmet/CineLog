package com.atamanahmet.cinelog.dto;

import java.util.List;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body sent to the recommendation engine. Null mediaType means movies and TV.
 * Null minScore, exclude, or genre lists omit the field so the engine keeps its defaults.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationRequestPayload(
        @JsonProperty("loved") List<RecommendationLovedPayload> loved,
        @JsonProperty("media_type") TmdbMediaType mediaType,
        @JsonProperty("limit") Integer limit,
        @JsonProperty("min_score") Double minScore,
        @JsonProperty("exclude") List<RecommendationLovedPayload> exclude,
        @JsonProperty("genre_include") List<Integer> genreInclude,
        @JsonProperty("genre_exclude") List<Integer> genreExclude) {

    public RecommendationRequestPayload {
        if (loved == null) {
            throw new IllegalArgumentException("loved must not be null");
        }
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
