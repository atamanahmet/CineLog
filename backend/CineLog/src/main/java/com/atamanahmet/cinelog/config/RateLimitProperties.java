package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private final BucketLimit tmdb = new BucketLimit(25, 25, 1);
    private final BucketLimit tmdbWarm = new BucketLimit(3, 3, 1);
    private final BucketLimit ip = new BucketLimit(30, 30, 60);
    private final BucketLimit auth = new BucketLimit(5, 5, 60);
    private long tmdbMaxWaitSeconds = 1;
    private long tmdbWarmMaxWaitSeconds = 5;
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

        public BucketLimit() {
        }

        public BucketLimit(long capacity, long refillTokens, long refillDurationSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDurationSeconds = refillDurationSeconds;
        }

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
