package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Discover/search TV payload returned by the API and stored in cache.
 */
public record TvShowDto(
        Integer id,
        Boolean adult,
        String backdropPath,
        List<Integer> genreIds,
        List<String> originCountry,
        String originalLanguage,
        String originalTitle,
        String overview,
        Double popularity,
        String posterPath,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate,
        String title,
        Double voteAverage,
        Integer voteCount,
        String status,
        Integer numberOfSeasons,
        Integer numberOfEpisodes,
        Integer episodeRunTime,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate firstAirDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate lastAirDate,
        List<String> networks,
        List<TvCreatedByDto> createdBy,
        TvNextEpisodeDto nextEpisodeToAir,
        List<TvSeasonSummaryDto> seasons) {

    /**
     * Treat null lists as empty. Series fields stay null when unknown.
     */
    public TvShowDto {
        genreIds = genreIds == null ? List.of() : genreIds;
        originCountry = originCountry == null ? List.of() : originCountry;
        networks = networks == null ? List.of() : networks;
        createdBy = createdBy == null ? List.of() : createdBy;
        seasons = seasons == null ? List.of() : seasons;
    }
}
