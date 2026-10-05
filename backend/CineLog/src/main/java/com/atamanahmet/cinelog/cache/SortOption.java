package com.atamanahmet.cinelog.cache;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

/**
 * TMDB discover sort options, asc and desc for each field.
 */
public enum SortOption {

    POPULARITY_ASC("popularity.asc"),
    POPULARITY_DESC("popularity.desc"),
    RATING_ASC("vote_average.asc"),
    RATING_DESC("vote_average.desc"),
    RELEASE_DATE_ASC("primary_release_date.asc", "first_air_date.asc", "release_date.asc"),
    RELEASE_DATE_DESC("primary_release_date.desc", "first_air_date.desc", "release_date.desc"),
    VOTE_COUNT_ASC("vote_count.asc"),
    VOTE_COUNT_DESC("vote_count.desc"),
    TITLE_ASC("title.asc", "name.asc"),
    TITLE_DESC("title.desc", "name.desc");

    private final String movieSort;
    private final String tvSort;
    private final String legacyAlias;

    SortOption(String sort) {
        this(sort, sort, null);
    }

    SortOption(String movieSort, String tvSort) {
        this(movieSort, tvSort, null);
    }

    SortOption(String movieSort, String tvSort, String legacyAlias) {
        this.movieSort = movieSort;
        this.tvSort = tvSort;
        this.legacyAlias = legacyAlias;
    }

    /**
     * Return the TMDB sort_by value for this media type.
     */
    public String toTmdbSort(TmdbMediaType mediaType) {
        return mediaType == TmdbMediaType.MOVIE ? movieSort : tvSort;
    }

    /**
     * Match a TMDB sort_by string or enum name, or throw if unknown.
     */
    public static SortOption fromRequest(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        for (SortOption option : values()) {
            if (option.movieSort.equals(sort) || option.tvSort.equals(sort) || sort.equals(option.legacyAlias)) {
                return option;
            }
        }
        try {
            return SortOption.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown sort: " + sort, e);
        }
    }
}
