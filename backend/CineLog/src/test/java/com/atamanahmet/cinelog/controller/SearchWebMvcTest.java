package com.atamanahmet.cinelog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.config.WebMvcConfig;
import com.atamanahmet.cinelog.dto.SearchPageResponse;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.TvShowService;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = { MovieController.class, TvController.class }, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ ApiExceptionHandler.class, WebMvcConfig.class })
class SearchWebMvcTest {

    private static final String SPECIAL_QUERY = "foo/bar & baz? qux";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private TvShowService tvShowService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GenreCacheService genreCacheService;

    /**
     * Movie search returns 200 and never calls the user service.
     */
    @Test
    void movieSearchDoesNotCallUserService() throws Exception {
        when(movieService.searchMovies(eq("matrix"), eq(1), isNull())).thenReturn(emptyMoviePage());
        mockMvc.perform(get("/api/movie/search").param("query", "matrix"))
                .andExpect(status().isOk());
        verify(movieService).searchMovies("matrix", 1, null);
        verifyNoInteractions(userService);
    }

    /**
     * TV search returns 200 and never calls the user service.
     */
    @Test
    void tvSearchDoesNotCallUserService() throws Exception {
        when(tvShowService.searchTvShows(eq("office"), eq(1), isNull())).thenReturn(emptyTvPage());
        mockMvc.perform(get("/api/tv/search").param("query", "office"))
                .andExpect(status().isOk());
        verify(tvShowService).searchTvShows("office", 1, null);
        verifyNoInteractions(userService);
    }

    /**
     * Slash, ampersand, question mark and spaces reach the movie service unchanged.
     */
    @Test
    void movieSpecialCharsReachServiceUnchanged() throws Exception {
        when(movieService.searchMovies(any(), anyInt(), nullable(Integer.class))).thenReturn(emptyMoviePage());
        mockMvc.perform(get("/api/movie/search").param("query", SPECIAL_QUERY))
                .andExpect(status().isOk());
        verify(movieService).searchMovies(SPECIAL_QUERY, 1, null);
    }

    /**
     * Movie page query param is forwarded to the service.
     */
    @Test
    void moviePageReachesService() throws Exception {
        when(movieService.searchMovies(eq("matrix"), eq(2), isNull()))
                .thenReturn(new SearchPageResponse<>(2, List.of(), 5, 100));
        mockMvc.perform(get("/api/movie/search").param("query", "matrix").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.totalResults").value(100));
        verify(movieService).searchMovies("matrix", 2, null);
    }

    /**
     * Negative movie minVotes is HTTP 400.
     */
    @Test
    void movieNegativeMinVotesReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/search").param("query", "matrix").param("minVotes", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(movieService);
    }

    /**
     * Movie minVotes above the Discover bound is HTTP 400.
     */
    @Test
    void movieMinVotesAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/search")
                        .param("query", "matrix")
                        .param("minVotes", String.valueOf(DiscoverRequest.MAX_VOTE_COUNT + 1)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(movieService);
    }

    /**
     * Valid movie minVotes returns 200 and reaches the service.
     */
    @Test
    void movieValidMinVotesReachesService() throws Exception {
        when(movieService.searchMovies(eq("matrix"), eq(1), eq(100))).thenReturn(emptyMoviePage());
        mockMvc.perform(get("/api/movie/search").param("query", "matrix").param("minVotes", "100"))
                .andExpect(status().isOk());
        verify(movieService).searchMovies("matrix", 1, 100);
    }

    /**
     * Missing movie query is HTTP 400.
     */
    @Test
    void movieMissingQueryReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/search")).andExpect(status().isBadRequest());
    }

    /**
     * Blank movie query is HTTP 400.
     */
    @Test
    void movieBlankQueryReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    /**
     * Movie query longer than 100 characters is HTTP 400.
     */
    @Test
    void movieQueryOver100Returns400() throws Exception {
        mockMvc.perform(get("/api/movie/search").param("query", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Movie page below 1 is HTTP 400.
     */
    @Test
    void moviePageBelowOneReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/search").param("query", "matrix").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Slash, ampersand, question mark and spaces reach the TV service unchanged.
     */
    @Test
    void tvSpecialCharsReachServiceUnchanged() throws Exception {
        when(tvShowService.searchTvShows(any(), anyInt(), nullable(Integer.class))).thenReturn(emptyTvPage());
        mockMvc.perform(get("/api/tv/search").param("query", SPECIAL_QUERY))
                .andExpect(status().isOk());
        verify(tvShowService).searchTvShows(SPECIAL_QUERY, 1, null);
    }

    /**
     * TV page query param is forwarded to the service.
     */
    @Test
    void tvPageReachesService() throws Exception {
        when(tvShowService.searchTvShows(eq("office"), eq(3), isNull()))
                .thenReturn(new SearchPageResponse<>(3, List.of(), 8, 160));
        mockMvc.perform(get("/api/tv/search").param("query", "office").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.totalPages").value(8))
                .andExpect(jsonPath("$.totalResults").value(160));
        verify(tvShowService).searchTvShows("office", 3, null);
    }

    /**
     * Negative TV minVotes is HTTP 400.
     */
    @Test
    void tvNegativeMinVotesReturns400() throws Exception {
        mockMvc.perform(get("/api/tv/search").param("query", "office").param("minVotes", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tvShowService);
    }

    /**
     * TV minVotes above the Discover bound is HTTP 400.
     */
    @Test
    void tvMinVotesAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/tv/search")
                        .param("query", "office")
                        .param("minVotes", String.valueOf(DiscoverRequest.MAX_VOTE_COUNT + 1)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tvShowService);
    }

    /**
     * Valid TV minVotes returns 200 and reaches the service.
     */
    @Test
    void tvValidMinVotesReachesService() throws Exception {
        when(tvShowService.searchTvShows(eq("office"), eq(1), eq(100))).thenReturn(emptyTvPage());
        mockMvc.perform(get("/api/tv/search").param("query", "office").param("minVotes", "100"))
                .andExpect(status().isOk());
        verify(tvShowService).searchTvShows("office", 1, 100);
    }

    /**
     * Missing TV query is HTTP 400.
     */
    @Test
    void tvMissingQueryReturns400() throws Exception {
        mockMvc.perform(get("/api/tv/search")).andExpect(status().isBadRequest());
    }

    /**
     * Blank TV query is HTTP 400.
     */
    @Test
    void tvBlankQueryReturns400() throws Exception {
        mockMvc.perform(get("/api/tv/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    /**
     * TV query longer than 100 characters is HTTP 400.
     */
    @Test
    void tvQueryOver100Returns400() throws Exception {
        mockMvc.perform(get("/api/tv/search").param("query", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    private static SearchPageResponse<com.atamanahmet.cinelog.dto.MovieDto> emptyMoviePage() {
        return new SearchPageResponse<>(1, List.of(), 0, 0);
    }

    private static SearchPageResponse<com.atamanahmet.cinelog.dto.TvShowDto> emptyTvPage() {
        return new SearchPageResponse<>(1, List.of(), 0, 0);
    }
}
