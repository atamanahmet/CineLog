package com.atamanahmet.cinelog.client.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TmdbClientTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    @Test
    void pastEndYearGivesLastDayOfThatYear() {
        assertEquals("2020-12-31", TmdbClient.upperReleaseBound(2020, TODAY));
    }

    @Test
    void currentEndYearGivesToday() {
        assertEquals("2026-09-20", TmdbClient.upperReleaseBound(2026, TODAY));
    }

    @Test
    void futureEndYearGivesToday() {
        assertEquals("2026-09-20", TmdbClient.upperReleaseBound(2040, TODAY));
    }

    @Test
    void nullEndYearGivesToday() {
        assertEquals("2026-09-20", TmdbClient.upperReleaseBound(null, TODAY));
    }
}
