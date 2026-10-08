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
 * AuthProperties binding, TTL defaults, and required CORS origins.
 */
class AuthPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableAuth.class)
            .withPropertyValues("auth.allowed-origins=http://localhost");

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class EnableAuth {
    }

    @Test
    void accessTtlIs15MinutesAndRefreshIs30Days() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            AuthProperties props = context.getBean(AuthProperties.class);
            assertEquals(Duration.ofMinutes(15), props.accessTokenTtl());
            assertEquals(Duration.ofDays(30), props.refreshTokenTtl());
        });
    }

    @Test
    void blankAllowedOriginsFailsStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withUserConfiguration(EnableAuth.class)
                .withPropertyValues("auth.allowed-origins=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingAllowedOriginsFailsStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withUserConfiguration(EnableAuth.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
