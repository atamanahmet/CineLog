package com.atamanahmet.cinelog.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.atamanahmet.cinelog.domain.POJO.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
                .sign(Algorithm.HMAC512(secretKey));
    }

    public boolean validateToken(String token, User user) {
        return extractUsername(token).equals(user.getUsername());
    }

    public String extractUsername(String token) {
        return JWT.require(Algorithm.HMAC512(secretKey))
                .build()
                .verify(token)
                .getSubject();
    }

    public String extractUsernameFromCookies(Cookie[] cookies) {
        if (cookies == null)
            return null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("jwt_token")) {
                return extractUsername(cookie.getValue());
            }
        }
        return null;
    }

    public String extractUsernameFromRequest(HttpServletRequest request) {
        return extractUsernameFromCookies(request.getCookies());
    }
}