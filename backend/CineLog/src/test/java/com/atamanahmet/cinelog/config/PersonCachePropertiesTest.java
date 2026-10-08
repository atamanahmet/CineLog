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
 * PersonCacheProperties defaults match today's literals.
 */
class PersonCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnablePerson.class);

    @Configuration
    @EnableConfigurationProperties(PersonCacheProperties.class)
    static class EnablePerson {
    }

    @Test
    void defaultsAre6TtlAnd500MaxSize() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            PersonCacheProperties props = context.getBean(PersonCacheProperties.class);
            assertEquals(6, props.getTtlHours());
            assertEquals(500, props.getMaxSize());
        });
    }
}
