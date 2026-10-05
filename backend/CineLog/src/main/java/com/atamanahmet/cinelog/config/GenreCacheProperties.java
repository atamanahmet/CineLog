package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "genre.cache")
public class GenreCacheProperties {

    private long ttlHours = 168;

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }
}
