package com.atamanahmet.cinelog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * RecommendationLimitsProperties binding and bounds.
 */
class RecommendationLimitsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableLimits.class);

    @Configuration
    @EnableConfigurationProperties(RecommendationLimitsProperties.class)
    static class EnableLimits {
    }

    @Test
    void defaultMaxResultsIs100() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertEquals(100, context.getBean(RecommendationLimitsProperties.class).getMaxResults());
        });
    }

    @Test
    void maxResultsBelow10FailsStartup() {
        runner.withPropertyValues("recommendation.limits.max-results=9")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void maxResultsAbove500FailsStartup() {
        runner.withPropertyValues("recommendation.limits.max-results=501")
                .run(context -> assertThat(context).hasFailed());
    }
}
