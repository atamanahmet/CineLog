package com.atamanahmet.cinelog.controller;

import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.domain.entity.RecommendationScope;
import com.atamanahmet.cinelog.dto.RecommendationSettingsDto;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.service.AccountService;

import jakarta.servlet.Filter;

/**
 * Settings GET includes the configured maxResultsLimit.
 */
@WebMvcTest(controllers = AccountController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class RecommendationSettingsResponseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void settingsResponseContainsMaxResultsLimit() throws Exception {
        when(accountService.getRecommendationSettings())
                .thenReturn(new RecommendationSettingsDto(RecommendationScope.ALL, 0.3, 50, 100));

        mockMvc.perform(get("/api/user/account/recommendation-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxResults").value(50))
                .andExpect(jsonPath("$.maxResultsLimit").value(100));
    }
}
