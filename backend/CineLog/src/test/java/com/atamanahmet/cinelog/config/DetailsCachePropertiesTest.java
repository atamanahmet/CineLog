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
 * DetailsCacheProperties defaults match today's literals.
 */
class DetailsCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnableDetails.class);

    @Configuration
    @EnableConfigurationProperties(DetailsCacheProperties.class)
    static class EnableDetails {
    }

    @Test
    void defaultsAre6TtlAnd2000MaxSize() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            DetailsCacheProperties props = context.getBean(DetailsCacheProperties.class);
            assertEquals(6, props.getTtlHours());
            assertEquals(2000, props.getMaxSize());
        });
    }
}
