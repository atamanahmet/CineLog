package com.atamanahmet.cinelog.client.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ReleaseDateWindowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    @Test
    void releasedNullEndYearGivesToday() {
        ReleaseDateWindow window = ReleaseDateWindow.released(new int[] { 1900 }, TODAY);
        assertEquals(LocalDate.of(1900, 1, 1), window.from());
        assertEquals(TODAY, window.to());
    }

    @Test
    void releasedFutureEndYearGivesToday() {
        ReleaseDateWindow window = ReleaseDateWindow.released(new int[] { 1900, 2040 }, TODAY);
        assertEquals(LocalDate.of(1900, 1, 1), window.from());
        assertEquals(TODAY, window.to());
    }

    @Test
    void releasedPastEndYearGivesLastDayOfThatYear() {
        ReleaseDateWindow window = ReleaseDateWindow.released(new int[] { 1900, 2020 }, TODAY);
        assertEquals(LocalDate.of(1900, 1, 1), window.from());
        assertEquals(LocalDate.of(2020, 12, 31), window.to());
    }

    @Test
    void upcomingGivesTomorrowAndPlusThreeYears() {
        ReleaseDateWindow window = ReleaseDateWindow.upcoming(TODAY);
        assertEquals(LocalDate.of(2026, 9, 21), window.from());
        assertEquals(LocalDate.of(2029, 9, 20), window.to());
    }

    @Test
    void upcomingHandlesMonthEnd() {
        LocalDate today = LocalDate.of(2026, 1, 31);
        ReleaseDateWindow window = ReleaseDateWindow.upcoming(today);
        assertEquals(LocalDate.of(2026, 2, 1), window.from());
        assertEquals(LocalDate.of(2029, 1, 31), window.to());
    }

    @Test
    void upcomingHandlesLeapDay() {
        LocalDate today = LocalDate.of(2024, 2, 29);
        ReleaseDateWindow window = ReleaseDateWindow.upcoming(today);
        assertEquals(LocalDate.of(2024, 3, 1), window.from());
        assertEquals(LocalDate.of(2027, 2, 28), window.to());
    }
}
