package com.atamanahmet.cinelog.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.client.RecommendationUnavailableException;
import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.UserDetailsImpl;
import com.atamanahmet.cinelog.service.ProfilePhotoService;
import com.atamanahmet.cinelog.service.RecommendationHydrator;
import com.atamanahmet.cinelog.service.RecommendationQualityFilter;
import com.atamanahmet.cinelog.service.RecommendationService;
import com.atamanahmet.cinelog.service.UserListService;
import com.atamanahmet.cinelog.service.UserService;
import com.atamanahmet.cinelog.service.impl.CurrentUserServiceImpl;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
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
class RecommendationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationClient recommendationClient;

    @MockitoBean
    private com.atamanahmet.cinelog.service.RecommendationExclusionService recommendationExclusionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserListService userListService;

    @MockitoBean
    private ProfilePhotoService profilePhotoService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CatalogCacheRepository catalogCacheRepository;

    @MockitoBean(name = "tmdbHydrateClient")
    private TmdbClient tmdbClient;

    @BeforeEach
    void stubLoggedInUserWithLovedTitle() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        UserDetailsImpl details = new UserDetailsImpl(1, "alice", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(user));
        when(userService.findLovedKeys(1)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationExclusionService.forEngine(any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Engine hits hydrate to a JSON array with HTTP 200.
     */
    @Test
    void hitsReturn200List() throws Exception {
        when(recommendationClient.requestRecommendations(any()))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        CatalogCache cache = new CatalogCache();
        cache.setTmdbId(99);
        cache.setMediaType(TmdbMediaType.MOVIE);
        cache.setTitle("Heat");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), any()))
                .thenReturn(List.of(cache));
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), any()))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99))
                .andExpect(jsonPath("$[0].title").value("Heat"))
                .andExpect(jsonPath("$[0].vote_average").value(nullValue()))
                .andExpect(jsonPath("$[0].media_type").value("MOVIE"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * Engine returning no hits is an empty array, not an error.
     */
    @Test
    void noHitsReturn200EmptyArray() throws Exception {
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Engine failure is HTTP 502 with a ProblemDetail body.
     */
    @Test
    void engineFailureReturns502Problem() throws Exception {
        when(recommendationClient.requestRecommendations(any()))
                .thenThrow(new RecommendationUnavailableException("Recommendation engine unavailable"));
        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "MOVIE"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("Bad Gateway"));
    }

    /**
     * Hits missing from catalog_cache are skipped; remaining rows keep engine score order.
     */
    @Test
    void missingCatalogCacheIdsAreSkippedKeepingOrder() throws Exception {
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(100, TmdbMediaType.MOVIE, 0.95),
                new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91),
                new RecommendationHitPayload(98, TmdbMediaType.TV, 0.80)));
        CatalogCache present = new CatalogCache();
        present.setTmdbId(99);
        present.setMediaType(TmdbMediaType.MOVIE);
        present.setTitle("Heat");
        CatalogCache later = new CatalogCache();
        later.setTmdbId(98);
        later.setMediaType(TmdbMediaType.TV);
        later.setTitle("Show");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), any()))
                .thenReturn(List.of(present));
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), any()))
                .thenReturn(List.of(later));
        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(99))
                .andExpect(jsonPath("$[1].id").value(98));
    }

    /**
     * MOVIE and TV GETs return different stubbed bodies, each with media_type and score.
     */
    @Test
    void movieAndTvReturnDifferentBodiesWithMediaTypeAndScore() throws Exception {
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenAnswer(invocation -> {
                    RecommendationRequestPayload payload = invocation.getArgument(0);
                    if (payload.mediaType() == TmdbMediaType.MOVIE) {
                        return List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91));
                    }
                    if (payload.mediaType() == TmdbMediaType.TV) {
                        return List.of(new RecommendationHitPayload(55, TmdbMediaType.TV, 0.77));
                    }
                    return List.of();
                });
        CatalogCache movieCache = new CatalogCache();
        movieCache.setTmdbId(99);
        movieCache.setMediaType(TmdbMediaType.MOVIE);
        movieCache.setTitle("Heat");
        CatalogCache tvCache = new CatalogCache();
        tvCache.setTmdbId(55);
        tvCache.setMediaType(TmdbMediaType.TV);
        tvCache.setTitle("Wire");
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), any()))
                .thenReturn(List.of(movieCache));
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), any()))
                .thenReturn(List.of(tvCache));

        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(99))
                .andExpect(jsonPath("$[0].media_type").value("MOVIE"))
                .andExpect(jsonPath("$[0].score").value(0.91));

        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "TV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(55))
                .andExpect(jsonPath("$[0].media_type").value("TV"))
                .andExpect(jsonPath("$[0].score").value(0.77));
    }

    /**
     * Missing mediaType query param is HTTP 400.
     */
    @Test
    void missingMediaTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/user/recommendation"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Invalid mediaType query param is HTTP 400.
     */
    @Test
    void invalidMediaTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/user/recommendation").param("mediaType", "BOOK"))
                .andExpect(status().isBadRequest());
    }
}
