package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Score hit returned by the recommendation engine (IDs only).
 */
public record RecommendationHitPayload(
        @JsonProperty("tmdb_id") Integer tmdbId,
        @JsonProperty("media_type") TmdbMediaType mediaType,
        double score) {

    public RecommendationHitPayload {
        if (tmdbId == null) {
            throw new IllegalArgumentException("tmdbId must not be null");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
    }
}
