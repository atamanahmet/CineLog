package com.atamanahmet.cinelog.security;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtCookieUtil {

    public void addJwtCookie(HttpServletResponse response, String token) {

        Cookie jwtCookie = new Cookie("jwt_token", token);

        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setDomain("localhost");
        jwtCookie.setAttribute("SameSite", "Strict");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60 * 1000);
        response.addCookie(jwtCookie);

    }

    public void removeJwtCookie(HttpServletResponse response) {

        Cookie jwtCookie = new Cookie("jwt_token", null);

        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setDomain("localhost");
        jwtCookie.setAttribute("SameSite", "Strict");
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
