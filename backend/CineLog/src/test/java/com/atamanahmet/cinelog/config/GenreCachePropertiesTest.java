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
 * GenreCacheProperties defaults match today's literals.
 */
class GenreCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnableGenre.class);

    @Configuration
    @EnableConfigurationProperties(GenreCacheProperties.class)
    static class EnableGenre {
    }

    @Test
    void defaultTtlIs168Hours() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertEquals(168, context.getBean(GenreCacheProperties.class).getTtlHours());
        });
    }
}
