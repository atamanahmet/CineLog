package com.atamanahmet.cinelog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * RecommendationQualityProperties binding and bounds.
 */
class RecommendationQualityPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableQuality.class);

    @Configuration
    @EnableConfigurationProperties(RecommendationQualityProperties.class)
    static class EnableQuality {
    }

    @Test
    void defaultsAreMovie100AndTv50() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RecommendationQualityProperties props = context.getBean(RecommendationQualityProperties.class);
            assertEquals(100, props.getMinVotesMovie());
            assertEquals(50, props.getMinVotesTv());
        });
    }

    @Test
    void negativeMovieFloorFailsStartup() {
        runner.withPropertyValues("recommendation.quality.min-votes-movie=-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void negativeTvFloorFailsStartup() {
        runner.withPropertyValues("recommendation.quality.min-votes-tv=-1")
                .run(context -> assertThat(context).hasFailed());
    }
}
