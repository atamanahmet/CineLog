package com.atamanahmet.cinelog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * RecommendationEngineProperties binding, timeout defaults, and required secrets.
 */
class RecommendationEnginePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableEngine.class)
            .withPropertyValues(
                    "recommendation.engine.url=http://localhost:8181/rec/update",
                    "recommendation.engine.update-token=test-update-token-xxxxxxxxxxxxxx");

    @Configuration
    @EnableConfigurationProperties(RecommendationEngineProperties.class)
    static class EnableEngine {
    }

    @Test
    void defaultTimeoutsAre3sConnectAnd10sRead() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RecommendationEngineProperties props = context.getBean(RecommendationEngineProperties.class);
            assertEquals(Duration.ofSeconds(3), props.connectTimeout());
            assertEquals(Duration.ofSeconds(10), props.readTimeout());
        });
    }

    @Test
    void blankUrlFailsStartup() {
        runner.withPropertyValues("recommendation.engine.url=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankUpdateTokenFailsStartup() {
        runner.withPropertyValues("recommendation.engine.update-token=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shortUpdateTokenFailsStartup() {
        runner.withPropertyValues("recommendation.engine.update-token=too-short")
                .run(context -> assertThat(context).hasFailed());
    }
}
