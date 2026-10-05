package com.atamanahmet.cinelog.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.dto.TvCreatedByDto;
import com.atamanahmet.cinelog.dto.TvNextEpisodeDto;
import com.atamanahmet.cinelog.dto.TvSeasonSummaryDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbCreatedBy;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenre;
import com.atamanahmet.cinelog.dto.tmdb.TmdbNetwork;
import com.atamanahmet.cinelog.dto.tmdb.TmdbNextEpisode;
import com.atamanahmet.cinelog.dto.tmdb.TmdbSeason;

/**
 * Shared MapStruct helpers for genre id lists and ISO date strings.
 */
@Component
public class MediaMappingHelper {

    /**
     * Prefer genre ids, else ids from genres, else an empty list.
     */
    public List<Integer> resolveGenreIds(List<Integer> genreIds, List<TmdbGenre> genres) {
        if (genreIds != null && !genreIds.isEmpty()) {
            return genreIds;
        }
        if (genres == null) {
            return List.of();
        }
        return genres.stream().map(TmdbGenre::id).toList();
    }

    /**
     * Parse an ISO local date string; blank becomes null.
     */
    public LocalDate stringToLocalDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * First runtime minutes value, or null when the list is empty.
     */
    @Named("firstEpisodeRunTime")
    public Integer firstEpisodeRunTime(List<Integer> episodeRunTime) {
        if (episodeRunTime == null || episodeRunTime.isEmpty()) {
            return null;
        }
        return episodeRunTime.get(0);
    }

    /**
     * Network names only.
     */
    @Named("networkNames")
    public List<String> networkNames(List<TmdbNetwork> networks) {
        if (networks == null || networks.isEmpty()) {
            return List.of();
        }
        return networks.stream()
                .map(TmdbNetwork::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    /**
     * Creator id and name pairs.
     */
    @Named("mapCreatedBy")
    public List<TvCreatedByDto> mapCreatedBy(List<TmdbCreatedBy> createdBy) {
        if (createdBy == null || createdBy.isEmpty()) {
            return List.of();
        }
        return createdBy.stream()
                .filter(row -> row.getId() != null && row.getName() != null && !row.getName().isBlank())
                .map(row -> new TvCreatedByDto(row.getId(), row.getName()))
                .toList();
    }

    /**
     * Next episode snapshot, or null when absent.
     */
    @Named("mapNextEpisode")
    public TvNextEpisodeDto mapNextEpisode(TmdbNextEpisode nextEpisode) {
        if (nextEpisode == null) {
            return null;
        }
        if (nextEpisode.getSeasonNumber() == null || nextEpisode.getEpisodeNumber() == null) {
            return null;
        }
        return new TvNextEpisodeDto(
                stringToLocalDate(nextEpisode.getAirDate()),
                nextEpisode.getSeasonNumber(),
                nextEpisode.getEpisodeNumber(),
                nextEpisode.getName());
    }

    /**
     * Season summaries without season 0.
     */
    @Named("mapSeasons")
    public List<TvSeasonSummaryDto> mapSeasons(List<TmdbSeason> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return List.of();
        }
        return seasons.stream()
                .filter(season -> season.getSeasonNumber() != null && season.getSeasonNumber() != 0)
                .map(this::toSeasonSummary)
                .toList();
    }

    private TvSeasonSummaryDto toSeasonSummary(TmdbSeason season) {
        return new TvSeasonSummaryDto(
                season.getSeasonNumber(),
                season.getName(),
                season.getEpisodeCount(),
                stringToLocalDate(season.getAirDate()),
                season.getPosterPath(),
                season.getVoteAverage());
    }
}
