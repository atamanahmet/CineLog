package com.atamanahmet.cinelog.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthRateLimitFilterPathTest {

    @Test
    void coversLoginAndRegister() {
        assertTrue(AuthRateLimitFilter.isRateLimitedPath("/api/auth/login"));
        assertTrue(AuthRateLimitFilter.isRateLimitedPath("/api/auth/register"));
    }

    @Test
    void ignoresOtherAuthAndNonAuthPaths() {
        assertFalse(AuthRateLimitFilter.isRateLimitedPath("/api/auth/me"));
        assertFalse(AuthRateLimitFilter.isRateLimitedPath("/api/auth/login/extra"));
        assertFalse(AuthRateLimitFilter.isRateLimitedPath("/api/movie/discover"));
        assertFalse(AuthRateLimitFilter.isRateLimitedPath("/actuator/health"));
    }
}
