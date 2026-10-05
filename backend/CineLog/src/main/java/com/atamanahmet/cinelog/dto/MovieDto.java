package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Discover/search movie payload returned by the API and stored in cache.
 */
public record MovieDto(
        Integer id,
        Boolean adult,
        String backdropPath,
        List<Integer> genreIds,
        String originalLanguage,
        String originalTitle,
        String overview,
        Double popularity,
        String posterPath,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate,
        String title,
        Boolean video,
        Double voteAverage,
        Integer voteCount) {

    /**
     * Treat a null genreIds list as empty.
     */
    public MovieDto {
        genreIds = genreIds == null ? List.of() : genreIds;
    }
}
