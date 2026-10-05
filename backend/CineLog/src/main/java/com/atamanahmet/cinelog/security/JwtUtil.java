package com.atamanahmet.cinelog.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.domain.entity.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtil {

    private final String secretKey;
    private final AuthProperties authProperties;

    public JwtUtil(@Value("${jwt.secret}") String secretKey, AuthProperties authProperties) {
        this.secretKey = secretKey;
        this.authProperties = authProperties;
    }

    public String generateToken(User user) {
        return generateToken(user.getUsername());
    }

    /**
     * Issue an access token for the given username.
     */
    public String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + authProperties.accessTokenTtl().toMillis()))
                .sign(Algorithm.HMAC512(secretKey));
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
