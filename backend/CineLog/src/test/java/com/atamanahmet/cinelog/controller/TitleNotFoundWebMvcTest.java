package com.atamanahmet.cinelog.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.atamanahmet.cinelog.service.DetailsBundle;
import com.atamanahmet.cinelog.service.DetailsCacheService;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.TvShowService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = { MovieController.class, TvController.class }, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ ApiExceptionHandler.class, DetailsCacheService.class, TitleNotFoundWebMvcTest.TestConfig.class })
class TitleNotFoundWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbClient tmdbClient;

    @MockitoBean
    private GenreCacheService genreCacheService;

    @MockitoBean
    private MovieMapper movieMapper;

    @MockitoBean
    private TvShowMapper tvShowMapper;

    /**
     * Upstream 404 on a movie id becomes HTTP 404 Not found.
     */
    @Test
    void missingMovieIdReturns404() throws Exception {
        when(tmdbClient.getMovieById(1)).thenThrow(tmdb("movie", "1", 404));
        mockMvc.perform(get("/api/movie/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    /**
     * Upstream 500 on a movie id stays HTTP 502.
     */
    @Test
    void movieUpstream500Returns502() throws Exception {
        when(tmdbClient.getMovieById(1)).thenThrow(tmdb("movie", "1", 500));
        mockMvc.perform(get("/api/movie/1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("TMDB request failed"));
    }

    /**
     * Upstream 404 on a TV id becomes HTTP 404 Not found.
     */
    @Test
    void missingTvIdReturns404() throws Exception {
        when(tmdbClient.getTvShowById(1)).thenThrow(tmdb("tv", "1", 404));
        mockMvc.perform(get("/api/tv/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    /**
     * Upstream 500 on a TV id stays HTTP 502.
     */
    @Test
    void tvUpstream500Returns502() throws Exception {
        when(tmdbClient.getTvShowById(1)).thenThrow(tmdb("tv", "1", 500));
        mockMvc.perform(get("/api/tv/1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("TMDB request failed"));
    }

    /**
     * A failed details load is not cached, so the client is called again.
     */
    @Test
    void failedMovieLoadIsNotCached() throws Exception {
        when(tmdbClient.getMovieById(2)).thenThrow(tmdb("movie", "2", 404));
        mockMvc.perform(get("/api/movie/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/movie/2")).andExpect(status().isNotFound());
        verify(tmdbClient, times(2)).getMovieById(2);
    }

    /**
     * A failed TV details load is not cached, so the client is called again.
     */
    @Test
    void failedTvLoadIsNotCached() throws Exception {
        when(tmdbClient.getTvShowById(2)).thenThrow(tmdb("tv", "2", 404));
        mockMvc.perform(get("/api/tv/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tv/2")).andExpect(status().isNotFound());
        verify(tmdbClient, times(2)).getTvShowById(2);
    }

    private static TmdbClientException tmdb(String mediaType, String id, int status) {
        return new TmdbClientException(mediaType, id, "TMDB request failed", null, status);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(name = CacheConfig.MOVIE_DETAILS_CACHE)
        Cache<Integer, DetailsBundle<MovieDto>> movieDetailsCache() {
            return Caffeine.newBuilder().build();
        }

        @Bean(name = CacheConfig.TV_DETAILS_CACHE)
        Cache<Integer, DetailsBundle<TvShowDto>> tvDetailsCache() {
            return Caffeine.newBuilder().build();
        }

        @Bean
        MovieService movieService(DetailsCacheService detailsCacheService) {
            return org.mockito.Mockito.mock(MovieService.class, invocation -> {
                if ("getDetailsFromTmdb".equals(invocation.getMethod().getName())) {
                    return detailsCacheService.getMovieDetails(invocation.getArgument(0));
                }
                return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
            });
        }

        @Bean
        TvShowService tvShowService(DetailsCacheService detailsCacheService) {
            return org.mockito.Mockito.mock(TvShowService.class, invocation -> {
                if ("getDetailsFromTmdb".equals(invocation.getMethod().getName())) {
                    return detailsCacheService.getTvShowDetails(invocation.getArgument(0));
                }
                return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
            });
        }
    }
}
