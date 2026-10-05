package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.domain.entity.RecommendationScope;
import com.atamanahmet.cinelog.validation.WithinMaxResults;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * User-selectable recommendation engine settings.
 */
public record RecommendationSettingsDto(
        @NotNull RecommendationScope scope,
        @NotNull @DecimalMin("0.1") @DecimalMax("0.9") Double minScore,
        @NotNull @Min(10) @WithinMaxResults Integer maxResults,
        Integer maxResultsLimit) {
}
