package com.atamanahmet.cinelog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = DiscoverController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class DiscoverDefaultsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Defaults endpoint returns 200 with bounds matching DiscoverRequest constants.
     */
    @Test
    void defaultsReturns200AndShape() throws Exception {
        mockMvc.perform(get("/api/discover/defaults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minVotes.default").value(DiscoverRequest.DEFAULT_MIN_VOTES))
                .andExpect(jsonPath("$.minVotes.max").value(DiscoverRequest.MAX_VOTE_COUNT))
                .andExpect(jsonPath("$.runtime.min").value(DiscoverRequest.MIN_RUNTIME))
                .andExpect(jsonPath("$.runtime.max").value(DiscoverRequest.MAX_RUNTIME))
                .andExpect(jsonPath("$.year.min").value(DiscoverRequest.MIN_YEAR))
                .andExpect(jsonPath("$.year.max").value(DiscoverRequest.MAX_YEAR))
                .andExpect(jsonPath("$.rating.min").value(DiscoverRequest.MIN_RATING))
                .andExpect(jsonPath("$.rating.max").value(DiscoverRequest.MAX_RATING))
                .andExpect(jsonPath("$.maxPage").value(DiscoverRequest.TMDB_MAX_PAGE));
    }
}
