package com.atamanahmet.cinelog.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recommendation engine URL, update auth token, and HTTP timeouts.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.engine")
public record RecommendationEngineProperties(
        @NotBlank String url,
        @NotBlank @Size(min = 32) String updateToken,
        @DefaultValue("3s") @NotNull Duration connectTimeout,
        @DefaultValue("10s") @NotNull Duration readTimeout) {
}
