package com.atamanahmet.cinelog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.UserDetailsServiceImpl;
import com.atamanahmet.cinelog.security.config.SecurityConfig;
import com.atamanahmet.cinelog.security.filter.JwtAuthFilter;
import com.atamanahmet.cinelog.service.AuthService;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, JwtCookieUtil.class })
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-hmac512",
        "auth.access-token-ttl=3600000ms",
        "auth.refresh-token-ttl=7d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost"
})
class AuthLogoutWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * GET logout is not the logout matcher, so cookies stay and nothing is revoked.
     */
    @Test
    void getLogoutDoesNotClearCookiesOrRevoke() throws Exception {
        mockMvc.perform(get("/api/auth/logout")
                .cookie(new Cookie("jwt_token", "access-value"))
                .cookie(refreshCookie("refresh-value")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("jwt_token"))
                .andExpect(cookie().doesNotExist("refresh_token"));

        verify(refreshTokenService, never()).revokeFamilyByRawToken(any());
    }

    /**
     * POST logout returns 200 and clears both cookies with the attributes used when setting them.
     */
    @Test
    void postLogoutReturns200AndClearsBothCookies() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie("jwt_token", "access-value"))
                .cookie(refreshCookie("refresh-value")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("jwt_token", 0))
                .andExpect(cookie().path("jwt_token", "/"))
                .andExpect(cookie().httpOnly("jwt_token", true))
                .andExpect(cookie().secure("jwt_token", false))
                .andExpect(cookie().sameSite("jwt_token", "Lax"))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(cookie().path("refresh_token", "/api/auth"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", false))
                .andExpect(cookie().sameSite("refresh_token", "Lax"));

        verify(refreshTokenService).revokeFamilyByRawToken("refresh-value");
    }

    private static Cookie refreshCookie(String value) {
        Cookie cookie = new Cookie("refresh_token", value);
        cookie.setPath("/api/auth");
        return cookie;
    }
}
