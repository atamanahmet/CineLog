package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One seed title for find-similar. Uniqueness is by (tmdbId, mediaType).
 */
public record SimilarSeedDto(
        @NotNull @Positive Integer tmdbId,
        @NotNull TmdbMediaType mediaType) {
}
