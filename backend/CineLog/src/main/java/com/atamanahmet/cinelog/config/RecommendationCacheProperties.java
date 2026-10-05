package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recommendation.cache")
public class RecommendationCacheProperties {

    private long ttlHours = 6;
    private int maxSize = 5000;
    private int hydratePoolSize = 8;
    private long hydrateTimeoutSeconds = 8;

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getHydratePoolSize() {
        return hydratePoolSize;
    }

    public void setHydratePoolSize(int hydratePoolSize) {
        this.hydratePoolSize = hydratePoolSize;
    }

    public long getHydrateTimeoutSeconds() {
        return hydrateTimeoutSeconds;
    }

    public void setHydrateTimeoutSeconds(long hydrateTimeoutSeconds) {
        this.hydrateTimeoutSeconds = hydrateTimeoutSeconds;
    }
}
