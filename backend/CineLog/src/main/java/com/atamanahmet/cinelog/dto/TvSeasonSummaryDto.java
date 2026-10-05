package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * One season row for TV details API. Season 0 is omitted in the mapper.
 */
public record TvSeasonSummaryDto(
        Integer seasonNumber,
        String name,
        Integer episodeCount,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate airDate,
        String posterPath,
        Double voteAverage) {
}
