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
 * DiscoverCacheProperties defaults match today's literals.
 */
class DiscoverCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EnableDiscover.class);

    @Configuration
    @EnableConfigurationProperties(DiscoverCacheProperties.class)
    static class EnableDiscover {
    }

    @Test
    void defaultsAre500Items6Ttl5Refresh() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            DiscoverCacheProperties props = context.getBean(DiscoverCacheProperties.class);
            assertEquals(500, props.getItemsPerSort());
            assertEquals(6, props.getTtlHours());
            assertEquals(5, props.getRefreshHours());
        });
    }
}
