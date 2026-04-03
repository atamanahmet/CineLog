package com.atamanahmet.cinelog.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtLogoutHandler implements LogoutHandler {

    @Autowired
    private JwtCookieUtil jwtCookieUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        jwtCookieUtil.removeJwtCookie(response);
    }
}
