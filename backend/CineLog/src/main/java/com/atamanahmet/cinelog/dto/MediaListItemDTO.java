package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;

import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Frontend-facing list membership row for either a movie or TV item.
 * Field names match RecommendationItemDTO (minus score).
 */
public record MediaListItemDTO(
        Integer id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        String posterPath,
        @JsonProperty("release_date") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate,
        String overview,
        @JsonProperty("media_type") TmdbMediaType mediaType) {

    public MediaListItemDTO {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
        if (originalTitle == null || originalTitle.isBlank()) {
            originalTitle = title;
        }
    }

    /**
     * Build a list item from a persisted movie.
     */
    public static MediaListItemDTO fromMovie(Movie movie) {
        return new MediaListItemDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getOriginalTitle(),
                movie.getPosterPath(),
                movie.getReleaseDate(),
                movie.getOverview(),
                TmdbMediaType.MOVIE);
    }

    /**
     * Build a list item from a persisted TV show.
     */
    public static MediaListItemDTO fromTvShow(TvShow tvShow) {
        return new MediaListItemDTO(
                tvShow.getId(),
                tvShow.getTitle(),
                tvShow.getOriginalTitle(),
                tvShow.getPosterPath(),
                tvShow.getReleaseDate(),
                tvShow.getOverview(),
                TmdbMediaType.TV);
    }
}
