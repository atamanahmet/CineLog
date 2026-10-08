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
 * CloudinaryProperties binding and timeout default.
 */
class CloudinaryPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableCloudinary.class);

    @Configuration
    @EnableConfigurationProperties(CloudinaryProperties.class)
    static class EnableCloudinary {
    }

    @Test
    void defaultTimeoutIs10Seconds() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertEquals(10, context.getBean(CloudinaryProperties.class).timeoutSeconds());
        });
    }

    @Test
    void zeroTimeoutFailsStartup() {
        runner.withPropertyValues("cloudinary.timeout-seconds=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
