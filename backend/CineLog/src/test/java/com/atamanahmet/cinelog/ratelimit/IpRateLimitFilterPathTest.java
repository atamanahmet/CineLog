package com.atamanahmet.cinelog.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpRateLimitFilterPathTest {

    @Test
    void coversDiscoverUnderMovieAndTvPrefixes() {
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/movie/discover"));
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/tv/discover"));
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/discover/defaults"));
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/movie/42/video"));
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/tv/42/credits"));
    }

    @Test
    void coversListAllEndpoints() {
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/movies"));
        assertTrue(IpRateLimitFilter.isRateLimitedPath("/api/tvshows"));
    }

    @Test
    void ignoresBareAndAuthPaths() {
        assertFalse(IpRateLimitFilter.isRateLimitedPath("/api/movie"));
        assertFalse(IpRateLimitFilter.isRateLimitedPath("/api/tv"));
        assertFalse(IpRateLimitFilter.isRateLimitedPath("/api/auth/me"));
        assertFalse(IpRateLimitFilter.isRateLimitedPath("/api/auth/login"));
        assertFalse(IpRateLimitFilter.isRateLimitedPath("/actuator/health"));
    }
}
