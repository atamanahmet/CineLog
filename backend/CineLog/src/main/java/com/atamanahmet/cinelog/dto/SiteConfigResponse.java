package com.atamanahmet.cinelog.dto;

/**
 * Public feature flags the frontend reads at startup.
 */
public record SiteConfigResponse(boolean adultContentEnabled) {
}
