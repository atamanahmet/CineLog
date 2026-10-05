package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Vote-count floors for recommendation quality filtering.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.quality")
public class RecommendationQualityProperties {

    @Min(0)
    private int minVotesMovie = 100;

    @Min(0)
    private int minVotesTv = 50;

    public int getMinVotesMovie() {
        return minVotesMovie;
    }

    public void setMinVotesMovie(int minVotesMovie) {
        this.minVotesMovie = minVotesMovie;
    }

    public int getMinVotesTv() {
        return minVotesTv;
    }

    public void setMinVotesTv(int minVotesTv) {
        this.minVotesTv = minVotesTv;
    }
}
