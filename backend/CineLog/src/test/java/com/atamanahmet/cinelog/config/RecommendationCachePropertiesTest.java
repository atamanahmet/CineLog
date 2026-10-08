package com.atamanahmet.cinelog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * RecommendationCacheProperties defaults match today's literals (display cache cap included).
 */
class RecommendationCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnableRecCache.class);

    @Configuration
    @EnableConfigurationProperties(RecommendationCacheProperties.class)
    static class EnableRecCache {
    }

    @Test
    void defaultsAre6Ttl5000Max8Pool8Timeout() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RecommendationCacheProperties props = context.getBean(RecommendationCacheProperties.class);
            assertEquals(6, props.getTtlHours());
            assertEquals(5000, props.getMaxSize());
            assertEquals(8, props.getHydratePoolSize());
            assertEquals(8, props.getHydrateTimeoutSeconds());
        });
    }
}
