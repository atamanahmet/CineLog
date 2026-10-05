package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableConfigurationProperties({
        RateLimitProperties.class,
        AuthProperties.class,
        ContentPolicyProperties.class,
        RecommendationEngineProperties.class,
        RecommendationLimitsProperties.class,
        RecommendationQualityProperties.class })
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
