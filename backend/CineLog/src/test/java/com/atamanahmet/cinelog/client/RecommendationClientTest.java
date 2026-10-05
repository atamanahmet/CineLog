package com.atamanahmet.cinelog.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atamanahmet.cinelog.config.RecommendationEngineProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;

class RecommendationClientTest {

    private static final String URL = "http://localhost:8181/rec/update";
    private static final String TOKEN = "test-update-token-xxxxxxxxxxxxxx";

    private MockRestServiceServer server;
    private RecommendationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RecommendationClient(properties(), builder.build());
    }

    private static RecommendationEngineProperties properties() {
        return new RecommendationEngineProperties(
                URL, TOKEN, Duration.ofSeconds(3), Duration.ofSeconds(10));
    }

    @Test
    void mapsJsonListOn200() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
                          "media_type": "MOVIE",
                          "limit": 20
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess(
                        """
                        [{"tmdb_id": 99, "media_type": "MOVIE", "score": 0.91}]
                        """,
                        MediaType.APPLICATION_JSON));

        List<RecommendationHitPayload> hits = client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        TmdbMediaType.MOVIE,
                        20,
                        null, null, null, null));

        server.verify();
        assertThat(hits).containsExactly(
                new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91));
    }

    @Test
    void emptyJsonArrayReturnsEmptyList() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<RecommendationHitPayload> hits = client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.TV)),
                        null,
                        5,
                        null, null, null, null));

        server.verify();
        assertThat(hits).isEmpty();
    }

    @Test
    void emptyBodyThrows() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        null,
                        20,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(RecommendationUnavailableException.class);

        server.verify();
    }

    @Test
    void http401Throws() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        null,
                        20,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(RecommendationUnavailableException.class);

        server.verify();
    }

    @Test
    void http500Throws() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        null,
                        20,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(RecommendationUnavailableException.class);

        server.verify();
    }

    @Test
    void omitsNullMediaTypeAndMinScore() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "TV"}],
                          "limit": 5
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.TV)),
                        null,
                        5,
                        null, null, null, null));

        server.verify();
    }

    @Test
    void sendsMinScoreWhenPresent() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
                          "media_type": "MOVIE",
                          "limit": 20,
                          "min_score": 0.55
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        TmdbMediaType.MOVIE,
                        20,
                        0.55,
                        null,
                        null,
                        null));

        server.verify();
    }

    /**
     * Exclude field uses the same JSON names as loved items.
     */
    @Test
    void sendsExcludeWithLovedItemNames() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
                          "media_type": "MOVIE",
                          "limit": 20,
                          "exclude": [
                            {"tmdb_id": 603, "media_type": "MOVIE"},
                            {"tmdb_id": 1399, "media_type": "TV"}
                          ]
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        TmdbMediaType.MOVIE,
                        20,
                        null,
                        List.of(
                                new RecommendationLovedPayload(603, TmdbMediaType.MOVIE),
                                new RecommendationLovedPayload(1399, TmdbMediaType.TV)),
                        null,
                        null));

        server.verify();
    }

    /**
     * Null exclude is omitted from the JSON body.
     */
    @Test
    void omitsExcludeWhenNull() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
                          "media_type": "MOVIE",
                          "limit": 20
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        TmdbMediaType.MOVIE,
                        20,
                        null,
                        null,
                        null,
                        null));

        server.verify();
    }

    /**
     * Genre lists use snake_case and are omitted when null.
     */
    @Test
    void sendsGenreFiltersWhenPresent() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json(
                        """
                        {
                          "loved": [{"tmdb_id": 10, "media_type": "MOVIE"}],
                          "media_type": "MOVIE",
                          "limit": 20,
                          "genre_include": [878, 28],
                          "genre_exclude": [16]
                        }
                        """,
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.requestRecommendations(
                new RecommendationRequestPayload(
                        List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                        TmdbMediaType.MOVIE,
                        20,
                        null,
                        null,
                        List.of(878, 28),
                        List.of(16)));

        server.verify();
    }
}
