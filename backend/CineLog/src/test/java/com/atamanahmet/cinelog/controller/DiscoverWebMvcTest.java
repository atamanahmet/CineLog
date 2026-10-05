package com.atamanahmet.cinelog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.config.WebMvcConfig;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.TvShowService;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = { MovieController.class, TvController.class }, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ ApiExceptionHandler.class, WebMvcConfig.class })
class DiscoverWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private TvShowService tvShowService;

    @MockitoBean
    private GenreCacheService genreCacheService;

    /**
     * A valid discover query returns 200.
     */
    @Test
    void validRequestReturns200() throws Exception {
        when(movieService.discoverMovies(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/movie/discover")
                .param("page", "1")
                .param("sort", "popularity.desc")
                .param("voteCount", "500"))
                .andExpect(status().isOk());
    }

    /**
     * Repeated yearRange keys bind as a two-value array.
     */
    @Test
    void repeatedYearRangeKeysBind() throws Exception {
        when(movieService.discoverMovies(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/movie/discover")
                .param("yearRange", "1900")
                .param("yearRange", "2040"))
                .andExpect(status().isOk());
        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(movieService).discoverMovies(captor.capture());
        int[] years = captor.getValue().yearRange();
        org.junit.jupiter.api.Assertions.assertEquals(1900, years[0]);
        org.junit.jupiter.api.Assertions.assertEquals(2040, years[1]);
    }

    /**
     * Unknown sort is HTTP 400 with the errors map.
     */
    @Test
    void unknownSortReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover").param("sort", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.sort").exists());
    }

    /**
     * Reversed year range is HTTP 400 with the errors map.
     */
    @Test
    void reversedYearRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover")
                .param("yearRange", "2040")
                .param("yearRange", "1900"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.yearRange").exists());
    }

    /**
     * Rating above 10 is HTTP 400 with the errors map.
     */
    @Test
    void ratingOutOfRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover")
                .param("ratingRange", "0")
                .param("ratingRange", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.ratingRange").exists());
    }

    /**
     * voteCount below 0 is HTTP 400 with the errors map.
     */
    @Test
    void voteCountNegativeReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover").param("voteCount", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.voteCount").exists());
    }

    /**
     * voteCount above 10000 is HTTP 400 with the errors map.
     */
    @Test
    void voteCountAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover").param("voteCount", "10001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.voteCount").exists());
    }

    /**
     * Runtime min above max is HTTP 400.
     */
    @Test
    void runtimeMinAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover")
                .param("minRuntime", "120")
                .param("maxRuntime", "60"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.minRuntime").exists());
    }

    /**
     * Negative runtime is HTTP 400.
     */
    @Test
    void runtimeNegativeReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover").param("minRuntime", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.minRuntime").exists());
    }

    /**
     * Runtime above 400 is HTTP 400.
     */
    @Test
    void runtimeAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/movie/discover").param("maxRuntime", "401"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.maxRuntime").exists());
    }
}
