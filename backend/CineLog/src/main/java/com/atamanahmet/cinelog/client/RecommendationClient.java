package com.atamanahmet.cinelog.client;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.atamanahmet.cinelog.config.RecommendationEngineProperties;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecommendationClient {

    private final RecommendationEngineProperties properties;
    private final RestClient restClient;
    private final String endpointPath;

    public RecommendationClient(
            RecommendationEngineProperties properties,
            @Qualifier("recommendationRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.endpointPath = URI.create(properties.url()).getPath();
    }

    /**
     * Post loved keys plus user settings and return scored hits. Throws when the engine is unavailable.
     */
    public List<RecommendationHitPayload> requestRecommendations(RecommendationRequestPayload payload) {
        try {
            RecommendationHitPayload[] body = restClient.post()
                    .uri(properties.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.updateToken())
                    .body(payload)
                    .retrieve()
                    .body(RecommendationHitPayload[].class);
            if (body == null) {
                log.warn("Recommendation engine returned empty body at {}", endpointPath);
                throw new RecommendationUnavailableException("Recommendation engine unavailable");
            }
            return List.of(body);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                log.warn("Recommendation engine HTTP {} at {}", status, endpointPath);
            } else if (e.getStatusCode().is4xxClientError()) {
                log.error("Recommendation engine HTTP {} at {}", status, endpointPath);
            } else {
                log.warn("Recommendation engine HTTP {} at {}", status, endpointPath);
            }
            throw new RecommendationUnavailableException("Recommendation engine unavailable", e);
        } catch (RestClientException e) {
            log.warn("Recommendation engine request failed at {}", endpointPath);
            throw new RecommendationUnavailableException("Recommendation engine unavailable", e);
        }
    }
}
