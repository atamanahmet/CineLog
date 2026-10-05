package com.atamanahmet.cinelog.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.atamanahmet.cinelog.config.WebMvcConfig;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.UserDetailsServiceImpl;
import com.atamanahmet.cinelog.security.config.SecurityConfig;
import com.atamanahmet.cinelog.security.filter.JwtAuthFilter;
import com.atamanahmet.cinelog.service.CurrentUserService;
import com.atamanahmet.cinelog.service.ProfilePhotoService;
import com.atamanahmet.cinelog.service.RecommendationService;
import com.atamanahmet.cinelog.service.RefreshTokenService;
import com.atamanahmet.cinelog.service.UserListService;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, JwtCookieUtil.class, WebMvcConfig.class,
        ApiExceptionHandler.class })
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-hmac512",
        "auth.access-token-ttl=3600000ms",
        "auth.refresh-token-ttl=30d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost"
})
class AnonymousProtectedEndpointWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserListService userListService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private ProfilePhotoService profilePhotoService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * Anonymous GET of a protected user path is 401, not 403.
     */
    @Test
    void anonymousListsRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/user/lists"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Anonymous GET of one list type is 401.
     */
    @Test
    void anonymousSingleListRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/user/list/REJECTED"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Unknown listType on GET matches PUT conversion / auth behavior.
     */
    @Test
    void unknownListTypeOnGetMatchesPut() throws Exception {
        int getStatus = mockMvc.perform(get("/api/user/list/notalist"))
                .andReturn()
                .getResponse()
                .getStatus();
        int putStatus = mockMvc.perform(put("/api/user/list/movie/notalist/1"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertEquals(putStatus, getStatus);
    }
}