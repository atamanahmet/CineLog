package com.atamanahmet.cinelog.security.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.SecurityConstants;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.equals("/") || requestURI.equals("/register") || requestURI.equals("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("jwt_token")) {
                    token = cookie.getValue();
                }
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String user = JWT.require(Algorithm.HMAC512(SecurityConstants.SECRET_KEY))
                .build()
                .verify(token)
                .getSubject();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, Arrays.asList());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
