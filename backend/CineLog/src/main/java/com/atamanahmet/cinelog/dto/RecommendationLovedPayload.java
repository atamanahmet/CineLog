package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal loved-item key sent to the recommendation engine.
 */
public record RecommendationLovedPayload(
        @JsonProperty("tmdb_id") Integer tmdbId,
        @JsonProperty("media_type") TmdbMediaType mediaType) {

    public RecommendationLovedPayload {
        if (tmdbId == null) {
            throw new IllegalArgumentException("tmdbId must not be null");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
    }
}
