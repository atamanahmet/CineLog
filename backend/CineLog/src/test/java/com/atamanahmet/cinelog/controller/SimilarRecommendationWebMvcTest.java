package com.atamanahmet.cinelog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.UserDetailsImpl;
import com.atamanahmet.cinelog.service.RecommendationHydrator;
import com.atamanahmet.cinelog.service.RecommendationQualityFilter;
import com.atamanahmet.cinelog.service.RecommendationService;
import com.atamanahmet.cinelog.service.UserService;
import com.atamanahmet.cinelog.service.impl.CurrentUserServiceImpl;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = SimilarRecommendationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties({
        RecommendationLimitsProperties.class,
        RecommendationQualityProperties.class
})
@TestPropertySource(properties = {
        "recommendation.limits.max-results=100",
        "recommendation.limits.max-exclude=10000"
})
@Import({
        RecommendationService.class,
        RecommendationHydrator.class,
        RecommendationQualityFilter.class,
        CurrentUserServiceImpl.class,
        ApiExceptionHandler.class,
        CacheConfig.class
})
class SimilarRecommendationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationClient recommendationClient;

    @MockitoBean
    private com.atamanahmet.cinelog.service.RecommendationExclusionService recommendationExclusionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CatalogCacheRepository catalogCacheRepository;

    @MockitoBean(name = "tmdbHydrateClient")
    private TmdbClient tmdbClient;

    @BeforeEach
    void stubLoggedInUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        UserDetailsImpl details = new UserDetailsImpl(1, "alice", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(user));
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of());
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(any(), any())).thenReturn(List.of());
        when(recommendationExclusionService.forEngine(any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Zero seeds is HTTP 400 through ApiExceptionHandler.
     */
    @Test
    void zeroSeedsReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendation/similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seeds":[],"mediaType":"MOVIE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Eleven seeds exceeds the named seed cap and is HTTP 400.
     */
    @Test
    void elevenSeedsReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendation/similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seeds":[
                                  {"tmdbId":1,"mediaType":"MOVIE"},
                                  {"tmdbId":2,"mediaType":"MOVIE"},
                                  {"tmdbId":3,"mediaType":"MOVIE"},
                                  {"tmdbId":4,"mediaType":"MOVIE"},
                                  {"tmdbId":5,"mediaType":"MOVIE"},
                                  {"tmdbId":6,"mediaType":"MOVIE"},
                                  {"tmdbId":7,"mediaType":"MOVIE"},
                                  {"tmdbId":8,"mediaType":"MOVIE"},
                                  {"tmdbId":9,"mediaType":"MOVIE"},
                                  {"tmdbId":10,"mediaType":"MOVIE"},
                                  {"tmdbId":11,"mediaType":"MOVIE"}
                                ],"mediaType":"MOVIE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Bad top-level mediaType is HTTP 400, same rule as GET /user/recommendation.
     */
    @Test
    void badMediaTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendation/similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seeds":[{"tmdbId":1,"mediaType":"MOVIE"}],"mediaType":"BOOK"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Duplicate (tmdbId, mediaType) seeds are HTTP 400.
     */
    @Test
    void duplicateSeedsReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendation/similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seeds":[
                                  {"tmdbId":1,"mediaType":"MOVIE"},
                                  {"tmdbId":1,"mediaType":"MOVIE"}
                                ],"mediaType":"MOVIE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Overlapping genreInclude and genreExclude is HTTP 400.
     */
    @Test
    void genreOverlapReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendation/similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seeds":[{"tmdbId":1,"mediaType":"MOVIE"}],
                                  "mediaType":"MOVIE",
                                  "genreInclude":[28],
                                  "genreExclude":[28]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
