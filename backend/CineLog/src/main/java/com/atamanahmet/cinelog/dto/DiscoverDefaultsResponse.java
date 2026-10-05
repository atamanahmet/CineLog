package com.atamanahmet.cinelog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Discover filter defaults and bounds for the frontend.
 */
public record DiscoverDefaultsResponse(
        MinVotesBounds minVotes,
        MinMaxBounds runtime,
        MinMaxBounds year,
        MinMaxBounds rating,
        int maxPage) {

    /**
     * Default and maximum vote count.
     */
    public record MinVotesBounds(@JsonProperty("default") int defaultValue, int max) {
    }

    /**
     * Inclusive minimum and maximum for a filter slider.
     */
    public record MinMaxBounds(int min, int max) {
    }
}
