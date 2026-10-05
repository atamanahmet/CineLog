package com.atamanahmet.cinelog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPersonResponse;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.mapper.PersonMapper;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.UserDetailsServiceImpl;
import com.atamanahmet.cinelog.security.config.SecurityConfig;
import com.atamanahmet.cinelog.security.filter.JwtAuthFilter;
import com.atamanahmet.cinelog.service.RefreshTokenService;
import com.atamanahmet.cinelog.service.impl.PersonServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = PersonController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtUtil.class,
        JwtCookieUtil.class,
        ApiExceptionHandler.class,
        PersonServiceImpl.class,
        PersonMapper.class,
        PersonWebMvcTest.TestConfig.class
})
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-hmac512",
        "auth.access-token-ttl=3600000ms",
        "auth.refresh-token-ttl=30d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost"
})
class PersonWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbClient tmdbClient;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * Upstream 404 on a person id becomes HTTP 404 Not found.
     */
    @Test
    void missingPersonIdReturns404() throws Exception {
        when(tmdbClient.getPersonById(1)).thenThrow(tmdb("person", "1", 404));
        mockMvc.perform(get("/api/person/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    /**
     * Anonymous GET of /api/person/{id} is allowed, same as movie details.
     */
    @Test
    void anonymousPersonRequestIsAllowed() throws Exception {
        when(tmdbClient.getPersonById(287)).thenReturn(samplePersonResponse());
        mockMvc.perform(get("/api/person/287"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.id").value(287))
                .andExpect(jsonPath("$.person.name").value("Brad Pitt"))
                .andExpect(jsonPath("$.credits").isArray());
    }

    private static TmdbClientException tmdb(String mediaType, String id, int status) {
        return new TmdbClientException(mediaType, id, "TMDB request failed", null, status);
    }

    private static TmdbPersonResponse samplePersonResponse() {
        TmdbPersonResponse response = new TmdbPersonResponse();
        response.setId(287);
        response.setName("Brad Pitt");
        response.setBiography("An actor.");
        response.setAlsoKnownAs(List.of());
        return response;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(name = CacheConfig.PERSON_CACHE)
        Cache<Integer, PersonDetailDto> personCache() {
            return Caffeine.newBuilder().build();
        }
    }
}
