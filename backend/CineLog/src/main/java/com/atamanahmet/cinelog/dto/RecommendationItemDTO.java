package com.atamanahmet.cinelog.dto;

import java.time.LocalDate;
import java.util.List;

import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Frontend-facing recommendation row: catalog_cache display fields plus rec-engine score.
 * Field names match the Movie JSON shape the profile UI already consumes.
 */
public record RecommendationItemDTO(
        Integer id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate,
        String overview,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("vote_count") Integer voteCount,
        Double score,
        @JsonProperty("media_type") TmdbMediaType mediaType,
        @JsonProperty("genre_ids") List<Integer> genreIds) {

    public RecommendationItemDTO {
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
        genreIds = genreIds == null ? List.of() : List.copyOf(genreIds);
    }

    /**
     * Build a response item from a catalog_cache row and its similarity score.
     */
    public static RecommendationItemDTO fromCache(CatalogCache cache, Double score) {
        return new RecommendationItemDTO(
                cache.getTmdbId(),
                cache.getTitle(),
                null,
                cache.getPosterPath(),
                cache.getReleaseDate(),
                cache.getOverview(),
                null,
                null,
                score,
                cache.getMediaType(),
                List.of());
    }

    /**
     * Build a response item from a TMDB display row and its similarity score.
     */
    public static RecommendationItemDTO fromDisplay(MediaDisplay display, Double score) {
        return new RecommendationItemDTO(
                display.tmdbId(),
                display.title(),
                null,
                display.posterPath(),
                display.releaseDate(),
                display.overview(),
                display.voteAverage(),
                display.voteCount(),
                score,
                display.mediaType(),
                display.genreIds());
    }
}
