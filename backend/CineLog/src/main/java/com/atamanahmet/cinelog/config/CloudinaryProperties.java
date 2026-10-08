package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Optional Cloudinary URL and HTTP timeout for profile photo uploads.
 */
@Validated
@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        @DefaultValue("") String url,
        @DefaultValue("10") @Min(1) int timeoutSeconds) {
}
