package com.atamanahmet.cinelog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-user recommendation engine settings stored on the users row.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationPreferences {

    public static final RecommendationScope DEFAULT_SCOPE = RecommendationScope.ALL;
    public static final double DEFAULT_MIN_SCORE = 0.3;
    public static final int DEFAULT_MAX_RESULTS = 50;

    @Enumerated(EnumType.STRING)
    @Column(name = "rec_scope", length = 16)
    private RecommendationScope scope;

    @Column(name = "rec_min_score")
    private Double minScore;

    @Column(name = "rec_max_results")
    private Integer maxResults;

    /**
     * Default settings for new users and rows with no saved preferences.
     */
    public static RecommendationPreferences defaults() {
        return new RecommendationPreferences(DEFAULT_SCOPE, DEFAULT_MIN_SCORE, DEFAULT_MAX_RESULTS);
    }

    /**
     * Copy with every null field replaced by its default.
     */
    public RecommendationPreferences withDefaults() {
        return new RecommendationPreferences(
                scope != null ? scope : DEFAULT_SCOPE,
                minScore != null ? minScore : DEFAULT_MIN_SCORE,
                maxResults != null ? maxResults : DEFAULT_MAX_RESULTS);
    }
}
