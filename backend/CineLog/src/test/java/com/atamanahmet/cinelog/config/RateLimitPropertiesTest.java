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
 * RateLimitProperties defaults match today's production literals.
 */
class RateLimitPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnableRateLimit.class);

    @Configuration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class EnableRateLimit {
    }

    @Test
    void defaultsMatchApplicationYmlLiterals() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RateLimitProperties props = context.getBean(RateLimitProperties.class);
            assertEquals(25, props.getTmdb().getCapacity());
            assertEquals(25, props.getTmdb().getRefillTokens());
            assertEquals(1, props.getTmdb().getRefillDurationSeconds());
            assertEquals(1, props.getTmdbMaxWaitSeconds());
            assertEquals(3, props.getTmdbWarm().getCapacity());
            assertEquals(3, props.getTmdbWarm().getRefillTokens());
            assertEquals(1, props.getTmdbWarm().getRefillDurationSeconds());
            assertEquals(5, props.getTmdbWarmMaxWaitSeconds());
            assertEquals(1, props.getTmdbRetryAfterDefaultSeconds());
            assertEquals(30, props.getIp().getCapacity());
            assertEquals(30, props.getIp().getRefillTokens());
            assertEquals(60, props.getIp().getRefillDurationSeconds());
            assertEquals(5, props.getAuth().getCapacity());
            assertEquals(5, props.getAuth().getRefillTokens());
            assertEquals(60, props.getAuth().getRefillDurationSeconds());
        });
    }
}
