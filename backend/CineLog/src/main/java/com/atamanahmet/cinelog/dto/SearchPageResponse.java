package com.atamanahmet.cinelog.dto;

import java.util.List;

/**
 * Paged search payload. Mirrors TMDB search fields the frontend needs for stop signals.
 */
public record SearchPageResponse<T>(
        Integer page,
        List<T> results,
        Integer totalPages,
        Integer totalResults) {

    public SearchPageResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
