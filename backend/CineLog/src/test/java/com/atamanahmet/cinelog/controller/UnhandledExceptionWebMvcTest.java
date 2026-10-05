package com.atamanahmet.cinelog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.UserDetailsServiceImpl;
import com.atamanahmet.cinelog.security.config.SecurityConfig;
import com.atamanahmet.cinelog.security.filter.JwtAuthFilter;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = MovieController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, JwtCookieUtil.class, ApiExceptionHandler.class })
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-hmac512",
        "auth.access-token-ttl=3600000ms",
        "auth.refresh-token-ttl=30d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost"
})
class UnhandledExceptionWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreCacheService genreCacheService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * Unexpected controller failure is HTTP 500, not a security 401 via /error.
     */
    @Test
    void unexpectedRuntimeExceptionReturns500Not401() throws Exception {
        when(movieService.getDetailsFromTmdb(1)).thenThrow(new RuntimeException("secret boom detail"));

        mockMvc.perform(get("/api/movie/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }
}
