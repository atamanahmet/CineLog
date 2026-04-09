package com.atamanahmet.cinelog.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtCookieUtil {

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public void addJwtCookie(HttpServletResponse response, String token) {

        Cookie jwtCookie = new Cookie("jwt_token", token);

        jwtCookie.setHttpOnly(true);
        // for https: true
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setAttribute("SameSite", "Lax");
        jwtCookie.setMaxAge((int) (jwtExpiration / 1000));
        response.addCookie(jwtCookie);

    }

    public void removeJwtCookie(HttpServletResponse response) {

        Cookie jwtCookie = new Cookie("jwt_token", null);

        jwtCookie.setHttpOnly(true);
        // for https: true
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setAttribute("SameSite", "Lax");
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);
    }

    public Cookie getJWTCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("jwt_token")) {
                return cookie;
            }
        }
        return null;
    }
}
