package com.atamanahmet.cinelog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.RecommendationScope;
import com.atamanahmet.cinelog.dto.RecommendationSettingsDto;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.service.AccountService;
import com.atamanahmet.cinelog.validation.WithinMaxResultsValidator;

import jakarta.servlet.Filter;

/**
 * Runtime maxResults cap from recommendation.limits.max-results.
 */
@WebMvcTest(controllers = AccountController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(RecommendationLimitsProperties.class)
@Import({ ApiExceptionHandler.class, WithinMaxResultsValidator.class })
@TestPropertySource(properties = "recommendation.limits.max-results=40")
class RecommendationSettingsMaxResultsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void maxResultsAtCapAccepted() throws Exception {
        when(accountService.updateRecommendationSettings(any(RecommendationSettingsDto.class)))
                .thenReturn(new RecommendationSettingsDto(RecommendationScope.ALL, 0.3, 40, 40));

        mockMvc.perform(put("/api/user/account/recommendation-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"ALL\",\"minScore\":0.3,\"maxResults\":40}"))
                .andExpect(status().isOk());
    }

    @Test
    void maxResultsAboveCapRejectedWith400() throws Exception {
        mockMvc.perform(put("/api/user/account/recommendation-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"ALL\",\"minScore\":0.3,\"maxResults\":41}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void maxResultsBelowMinRejectedWith400() throws Exception {
        mockMvc.perform(put("/api/user/account/recommendation-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"ALL\",\"minScore\":0.3,\"maxResults\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
