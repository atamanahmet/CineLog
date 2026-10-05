package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Operator caps for recommendation results and engine exclude lists.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.limits")
public class RecommendationLimitsProperties {

    /** Hard upper bound for the configurable result cap. */
    public static final int ABSOLUTE_MAX_RESULTS = 500;

    @Min(10)
    @Max(ABSOLUTE_MAX_RESULTS)
    private int maxResults = 100;

    @Min(1)
    private int maxExclude = 10000;

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxExclude() {
        return maxExclude;
    }

    public void setMaxExclude(int maxExclude) {
        this.maxExclude = maxExclude;
    }
}
