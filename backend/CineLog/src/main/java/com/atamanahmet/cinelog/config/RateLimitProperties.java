package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private final BucketLimit tmdb = new BucketLimit();
    private final BucketLimit tmdbWarm = new BucketLimit();
    private final BucketLimit ip = new BucketLimit();
    private final BucketLimit auth = new BucketLimit();
    private long tmdbMaxWaitSeconds = 1;
    private long tmdbWarmMaxWaitSeconds = 2;
    private long tmdbRetryAfterDefaultSeconds = 1;

    public BucketLimit getTmdb() {
        return tmdb;
    }

    public BucketLimit getTmdbWarm() {
        return tmdbWarm;
    }

    public BucketLimit getIp() {
        return ip;
    }

    public BucketLimit getAuth() {
        return auth;
    }

    public long getTmdbMaxWaitSeconds() {
        return tmdbMaxWaitSeconds;
    }

    public void setTmdbMaxWaitSeconds(long tmdbMaxWaitSeconds) {
        this.tmdbMaxWaitSeconds = tmdbMaxWaitSeconds;
    }

    public long getTmdbWarmMaxWaitSeconds() {
        return tmdbWarmMaxWaitSeconds;
    }

    public void setTmdbWarmMaxWaitSeconds(long tmdbWarmMaxWaitSeconds) {
        this.tmdbWarmMaxWaitSeconds = tmdbWarmMaxWaitSeconds;
    }

    public long getTmdbRetryAfterDefaultSeconds() {
        return tmdbRetryAfterDefaultSeconds;
    }

    public void setTmdbRetryAfterDefaultSeconds(long tmdbRetryAfterDefaultSeconds) {
        this.tmdbRetryAfterDefaultSeconds = tmdbRetryAfterDefaultSeconds;
    }

    public static class BucketLimit {
        private long capacity;
        private long refillTokens;
        private long refillDurationSeconds;

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillDurationSeconds() {
            return refillDurationSeconds;
        }

        public void setRefillDurationSeconds(long refillDurationSeconds) {
            this.refillDurationSeconds = refillDurationSeconds;
        }
    }
}
