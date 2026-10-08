package com.atamanahmet.cinelog.client.tmdb;

import java.time.LocalDate;

import com.atamanahmet.cinelog.dto.DiscoverRequest;

/**
 * Inclusive TMDB release / first-air date bounds for discover.
 */
public record ReleaseDateWindow(LocalDate from, LocalDate to) {

    public static final int UPCOMING_WINDOW_YEARS = 3;

    /**
     * Same bounds as the previous inline discover builders.
     */
    public static ReleaseDateWindow released(int[] yearRange, LocalDate today) {
        int startYear = yearRange != null && yearRange.length > 0
                ? yearRange[0]
                : DiscoverRequest.DEFAULT_YEAR_MIN;
        LocalDate from = LocalDate.parse(startYear + "-01-01");
        Integer endYear = yearRange != null && yearRange.length > 1 ? yearRange[1] : null;
        LocalDate to = LocalDate.parse(TmdbClient.upperReleaseBound(endYear, today));
        return new ReleaseDateWindow(from, to);
    }

    /**
     * Tomorrow through today plus UPCOMING_WINDOW_YEARS.
     */
    public static ReleaseDateWindow upcoming(LocalDate today) {
        return new ReleaseDateWindow(
                today.plusDays(1), today.plusYears(UPCOMING_WINDOW_YEARS));
    }
}
