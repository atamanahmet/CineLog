package com.atamanahmet.cinelog.dto.tmdb;

import java.time.LocalDate;
import java.util.List;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

/**
 * Light TMDB display fields for recommendation hydration.
 */
public record MediaDisplay(
        Integer tmdbId,
        TmdbMediaType mediaType,
        String title,
        String posterPath,
        LocalDate releaseDate,
        String overview,
        Double voteAverage,
        Integer voteCount,
        List<Integer> genreIds) {

    public MediaDisplay {
        if (tmdbId == null) {
            throw new IllegalArgumentException("tmdbId must not be null");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        genreIds = genreIds == null ? List.of() : List.copyOf(genreIds);
    }
}
