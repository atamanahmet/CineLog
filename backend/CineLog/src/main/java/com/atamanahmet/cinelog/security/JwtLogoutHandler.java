package com.atamanahmet.cinelog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final JwtCookieUtil jwtCookieUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie refreshCookie = jwtCookieUtil.getRefreshCookie(request);
        String rawToken = refreshCookie != null ? refreshCookie.getValue() : null;
        refreshTokenService.revokeFamilyByRawToken(rawToken);
        jwtCookieUtil.clearAuthCookies(response);
    }
}
