package com.atamanahmet.cinelog.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Auth token, cookie, and CORS settings.
 */
@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @DefaultValue("900000ms") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("Lax") String cookieSameSite,
        @NotEmpty List<String> allowedOrigins) {
}
