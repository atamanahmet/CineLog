package com.atamanahmet.cinelog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.client.tmdb.TmdbRateLimitExceededException;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.ratelimit.RateLimitExceptionHandler;
import com.atamanahmet.cinelog.service.AuthService;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.MovieService;

import jakarta.servlet.Filter;

@WebMvcTest(controllers = { AuthController.class, MovieController.class }, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ ApiExceptionHandler.class, RateLimitExceptionHandler.class })
class ApiExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreCacheService genreCacheService;

    /**
     * Malformed JSON body is HTTP 400, not the catch-all 500.
     */
    @Test
    void malformedJsonBodyReturns400Not500() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    /**
     * Non-numeric path id is HTTP 400, not the catch-all 500.
     */
    @Test
    void badPathVariableReturns400Not500() throws Exception {
        mockMvc.perform(get("/api/movie/not-an-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid movie id: not-an-id"));
    }

    /**
     * Unsupported HTTP method is HTTP 405, not the catch-all 500.
     */
    @Test
    void wrongHttpMethodReturns405Not500() throws Exception {
        mockMvc.perform(post("/api/movie/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Rate-limit advice still wins over the shared catch-all when both are loaded.
     */
    @Test
    void rateLimitExceptionReturns429WithRetryAfter() throws Exception {
        when(movieService.getDetailsFromTmdb(1))
                .thenThrow(new TmdbRateLimitExceededException(1_500_000_000L));

        mockMvc.perform(get("/api/movie/1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"));
    }
}
