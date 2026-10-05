package com.atamanahmet.cinelog.security.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

class PublicPathMatcherTest {

    private static boolean matches(String pattern, String path) {
        AntPathRequestMatcher matcher = new AntPathRequestMatcher(pattern);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return matcher.matches(request);
    }

    @Test
    void movieWildcardCoversDiscoverAndDetail() {
        assertTrue(matches("/api/movie/**", "/api/movie/discover"));
        assertTrue(matches("/api/movie/**", "/api/movie/123"));
        assertTrue(matches("/api/movie/**", "/api/movie/123/video"));
        // Spring Ant /** also matches the bare prefix path
        assertTrue(matches("/api/movie/**", "/api/movie"));
        assertFalse(matches("/api/movie/**", "/api/movies"));
    }

    @Test
    void tvWildcardCoversDiscoverAndDetail() {
        assertTrue(matches("/api/tv/**", "/api/tv/discover"));
        assertTrue(matches("/api/tv/**", "/api/tv/123"));
        assertTrue(matches("/api/tv/**", "/api/tv/123/credits"));
        assertTrue(matches("/api/tv/**", "/api/tv"));
        assertFalse(matches("/api/tv/**", "/api/tvshows"));
    }

    @Test
    void listAllExactMatchers() {
        assertTrue(matches("/api/movies", "/api/movies"));
        assertTrue(matches("/api/tvshows", "/api/tvshows"));
        assertFalse(matches("/api/movies", "/api/movie/discover"));
        assertFalse(matches("/api/tvshows", "/api/tv/discover"));
    }

    @Test
    void discoverWildcardCoversDefaults() {
        assertTrue(matches("/api/discover/**", "/api/discover/defaults"));
        assertTrue(matches("/api/discover/**", "/api/discover"));
        assertFalse(matches("/api/discover/**", "/api/movie/discover"));
    }
}
