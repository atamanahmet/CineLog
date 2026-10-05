package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Upcoming episode snapshot for TV details API.
 */
public record TvNextEpisodeDto(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate airDate,
        Integer seasonNumber,
        Integer episodeNumber,
        String name) {
}
