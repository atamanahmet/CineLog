package com.atamanahmet.cinelog.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RecommendationClientConfig {

    /**
     * RestClient for the recommendation engine with connect and read timeouts.
     */
    @Bean
    public RestClient recommendationRestClient(
            RestClient.Builder restClientBuilder,
            RecommendationEngineProperties properties) {
        return restClientBuilder
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(properties.connectTimeout())
                                .withReadTimeout(properties.readTimeout())))
                .build();
    }
}
