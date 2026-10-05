package com.atamanahmet.cinelog.dto;

import java.util.List;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

import org.hibernate.validator.constraints.UniqueElements;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Authenticated find-similar request seeded by explicit titles, not the loved list.
 */
public record SimilarRecommendationRequest(
        @NotNull
        @Size(min = 1, max = MAX_SEEDS)
        @UniqueElements
        @Valid
        List<SimilarSeedDto> seeds,
        @NotNull TmdbMediaType mediaType,
        List<Integer> genreInclude,
        List<Integer> genreExclude) {

    /** Hard cap on seed count for find-similar. */
    public static final int MAX_SEEDS = 10;

    public SimilarRecommendationRequest {
        genreInclude = genreInclude == null ? List.of() : List.copyOf(genreInclude);
        genreExclude = genreExclude == null ? List.of() : List.copyOf(genreExclude);
    }
}
