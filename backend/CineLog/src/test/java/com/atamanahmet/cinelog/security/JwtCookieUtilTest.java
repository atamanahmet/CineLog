package com.atamanahmet.cinelog.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.atamanahmet.cinelog.config.AuthProperties;

import jakarta.servlet.http.Cookie;

class JwtCookieUtilTest {

    /**
     * Access and refresh cookie attributes follow AuthProperties.
     */
    @Test
    void cookieAttributesFollowAuthProperties() {
        AuthProperties properties = new AuthProperties(
                Duration.ofHours(1),
                Duration.ofDays(7),
                true,
                "Strict",
                List.of("http://localhost:5173"));
        JwtCookieUtil util = new JwtCookieUtil(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        util.addJwtCookie(response, "access-token");
        util.addRefreshCookie(response, "refresh-token");

        Cookie access = response.getCookie("jwt_token");
        Cookie refresh = response.getCookie("refresh_token");

        assertEquals(3600, access.getMaxAge());
        assertTrue(access.getSecure());
        assertEquals("Strict", access.getAttribute("SameSite"));
        assertEquals("/", access.getPath());

        assertEquals(7 * 24 * 60 * 60, refresh.getMaxAge());
        assertTrue(refresh.getSecure());
        assertEquals("Strict", refresh.getAttribute("SameSite"));
        assertEquals("/api/auth", refresh.getPath());
    }

    /**
     * Dev defaults keep Secure false when properties say so.
     */
    @Test
    void insecureCookiesWhenPropertyFalse() {
        AuthProperties properties = new AuthProperties(
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                false,
                "Lax",
                List.of("http://localhost"));
        JwtCookieUtil util = new JwtCookieUtil(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        util.addJwtCookie(response, "access-token");
        util.addRefreshCookie(response, "refresh-token");

        assertFalse(response.getCookie("jwt_token").getSecure());
        assertFalse(response.getCookie("refresh_token").getSecure());
        assertEquals("Lax", response.getCookie("jwt_token").getAttribute("SameSite"));
        assertEquals("Lax", response.getCookie("refresh_token").getAttribute("SameSite"));
    }
}
