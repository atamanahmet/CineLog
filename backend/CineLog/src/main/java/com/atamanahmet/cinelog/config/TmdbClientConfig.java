package com.atamanahmet.cinelog.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.ratelimit.RateLimitBuckets;

import io.github.bucket4j.Bucket;

@Configuration
@EnableConfigurationProperties(RecommendationCacheProperties.class)
public class TmdbClientConfig {

    public static final String TMDB_RATE_LIMIT_BUCKET = "tmdbRateLimitBucket";
    public static final String TMDB_HYDRATE_CLIENT = "tmdbHydrateClient";

    /**
     * Shared outbound bucket for live TMDB calls and recommendation hydrate.
     */
    @Bean(name = TMDB_RATE_LIMIT_BUCKET)
    public Bucket tmdbRateLimitBucket(RateLimitProperties rateLimitProperties) {
        return RateLimitBuckets.create(rateLimitProperties.getTmdb());
    }

    /**
     * Interactive TMDB client. Waits up to tmdb-max-wait-seconds for a permit.
     */
    @Bean
    @Primary
    public TmdbClient tmdbApiClient(
            @Value("${api.key}") String apiKey,
            RestClient.Builder restClientBuilder,
            @Qualifier(TMDB_RATE_LIMIT_BUCKET) Bucket tmdbBucket,
            RateLimitProperties rateLimitProperties) {
        return new TmdbClient(
                apiKey,
                restClientBuilder,
                tmdbBucket,
                Duration.ofSeconds(rateLimitProperties.getTmdbMaxWaitSeconds()),
                false,
                Duration.ofSeconds(rateLimitProperties.getTmdbRetryAfterDefaultSeconds()));
    }

    /**
     * Hydrate TMDB client. Permit wait uses the remaining request deadline; retries one upstream 429.
     */
    @Bean(name = TMDB_HYDRATE_CLIENT)
    public TmdbClient tmdbHydrateClient(
            @Value("${api.key}") String apiKey,
            RestClient.Builder restClientBuilder,
            @Qualifier(TMDB_RATE_LIMIT_BUCKET) Bucket tmdbBucket,
            RecommendationCacheProperties recommendationCacheProperties,
            RateLimitProperties rateLimitProperties) {
        return new TmdbClient(
                apiKey,
                restClientBuilder,
                tmdbBucket,
                Duration.ofSeconds(recommendationCacheProperties.getHydrateTimeoutSeconds()),
                true,
                Duration.ofSeconds(rateLimitProperties.getTmdbRetryAfterDefaultSeconds()));
    }

    /**
     * Cache-warming TMDB client using the warm rate-limit bucket.
     */
    @Bean
    public TmdbClient tmdbWarmClient(
            @Value("${api.key}") String apiKey,
            RestClient.Builder restClientBuilder,
            RateLimitProperties rateLimitProperties) {
        Bucket bucket = RateLimitBuckets.create(rateLimitProperties.getTmdbWarm());
        return new TmdbClient(
                apiKey,
                restClientBuilder,
                bucket,
                Duration.ofSeconds(rateLimitProperties.getTmdbWarmMaxWaitSeconds()),
                false,
                Duration.ofSeconds(rateLimitProperties.getTmdbRetryAfterDefaultSeconds()));
    }
}
