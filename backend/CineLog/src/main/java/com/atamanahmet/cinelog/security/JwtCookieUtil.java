package com.atamanahmet.cinelog.security;

import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.config.AuthProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtCookieUtil {

    private static final String ACCESS_COOKIE = "jwt_token";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthProperties authProperties;

    public void addJwtCookie(HttpServletResponse response, String token) {
        Cookie jwtCookie = new Cookie(ACCESS_COOKIE, token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(authProperties.cookieSecure());
        jwtCookie.setPath("/");
        jwtCookie.setAttribute("SameSite", authProperties.cookieSameSite());
        jwtCookie.setMaxAge((int) authProperties.accessTokenTtl().toSeconds());
        response.addCookie(jwtCookie);
    }

    /**
     * HttpOnly refresh cookie, scoped to /api/auth.
     */
    public void addRefreshCookie(HttpServletResponse response, String rawRefreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, rawRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(authProperties.cookieSecure());
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setAttribute("SameSite", authProperties.cookieSameSite());
        cookie.setMaxAge((int) authProperties.refreshTokenTtl().toSeconds());
        response.addCookie(cookie);
    }

    public void removeJwtCookie(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie(ACCESS_COOKIE, null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(authProperties.cookieSecure());
        jwtCookie.setPath("/");
        jwtCookie.setAttribute("SameSite", authProperties.cookieSameSite());
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
    }

    /**
     * Clear the refresh cookie. Path must match the one used when setting it.
     */
    public void removeRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(authProperties.cookieSecure());
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setAttribute("SameSite", authProperties.cookieSameSite());
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Clear both access and refresh cookies.
     */
    public void clearAuthCookies(HttpServletResponse response) {
        removeJwtCookie(response);
        removeRefreshCookie(response);
    }

    public Cookie getJWTCookie(HttpServletRequest request) {
        return findCookie(request, ACCESS_COOKIE);
    }

    public Cookie getRefreshCookie(HttpServletRequest request) {
        return findCookie(request, REFRESH_COOKIE);
    }

    private static Cookie findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}
