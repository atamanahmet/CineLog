package com.atamanahmet.cinelog.client.tmdb;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atamanahmet.cinelog.dto.DiscoverRequest;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

class TmdbClientDiscoverTest {

    private static final String API_KEY = "test-tmdb-token-xxxxxxxxxxxxxxxx";
    private static final String EMPTY_PAGE = """
            {"page":1,"results":[],"total_pages":0,"total_results":0}
            """;

    private MockRestServiceServer server;
    private TmdbClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1000)
                        .refillGreedy(1000, Duration.ofSeconds(1))
                        .build())
                .build();
        client = new TmdbClient(API_KEY, builder, bucket, Duration.ofSeconds(1), false, Duration.ofSeconds(1));
    }

    /**
     * Movie discover sends vote_count.gte from the request voteCount.
     */
    @Test
    void movieDiscoverSendsVoteCountGteZero() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/movie")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("vote_count.gte", "0"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverMovies(new DiscoverRequest(false, "", 0, null, 1, null, null, null, "en", null, null, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE));

        server.verify();
    }

    /**
     * TV discover sends vote_count.gte from the request voteCount.
     */
    @Test
    void tvDiscoverSendsVoteCountGteZero() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/tv")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("vote_count.gte", "0"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverTvShows(new DiscoverRequest(false, "", 0, null, 1, null, null, null, null, null, null, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV));

        server.verify();
    }

    /**
     * Movie discover sends with_runtime bounds when set.
     */
    @Test
    void movieDiscoverSendsRuntimeWhenSet() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/movie")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("with_runtime.gte", "60"))
                .andExpect(queryParam("with_runtime.lte", "120"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverMovies(new DiscoverRequest(false, "", 0, null, 1, null, null, null, "en", 60, 120, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE));

        server.verify();
    }

    /**
     * TV discover sends with_runtime bounds when set.
     */
    @Test
    void tvDiscoverSendsRuntimeWhenSet() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/tv")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("with_runtime.gte", "60"))
                .andExpect(queryParam("with_runtime.lte", "120"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverTvShows(new DiscoverRequest(false, "", 0, null, 1, null, null, null, null, 60, 120, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV));

        server.verify();
    }

    /**
     * Movie discover omits with_runtime when unset.
     */
    @Test
    void movieDiscoverOmitsRuntimeWhenUnset() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/movie")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    String query = request.getURI().getQuery();
                    org.junit.jupiter.api.Assertions.assertFalse(query.contains("with_runtime"));
                })
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverMovies(new DiscoverRequest(false, "", 0, null, 1, null, null, null, "en", null, null, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE));

        server.verify();
    }

    /**
     * TV discover omits with_runtime when unset.
     */
    @Test
    void tvDiscoverOmitsRuntimeWhenUnset() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/tv")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    String query = request.getURI().getQuery();
                    org.junit.jupiter.api.Assertions.assertFalse(query.contains("with_runtime"));
                })
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverTvShows(new DiscoverRequest(false, "", 0, null, 1, null, null, null, null, null, null, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV));

        server.verify();
    }

    /**
     * Movie discover sends without_genres joined with the PIPE separator.
     */
    @Test
    void movieDiscoverSendsWithoutGenresPipe() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/movie")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    String query = java.net.URLDecoder.decode(
                            request.getURI().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            query.contains(
                                    "without_genres=16"
                                            + DiscoverRequest.TMDB_WITHOUT_GENRES_SEPARATOR
                                            + "35"),
                            query);
                })
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverMovies(new DiscoverRequest(
                        false, "", 0, null, 1, null, null, null, "en", null, null, java.util.List.of(16, 35), null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE));

        server.verify();
    }

    /**
     * TV discover omits without_genres when the exclude list is empty.
     */
    @Test
    void tvDiscoverOmitsWithoutGenresWhenEmpty() {
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/tv")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    String query = request.getURI().getQuery();
                    org.junit.jupiter.api.Assertions.assertFalse(query.contains("without_genres"));
                })
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverTvShows(new DiscoverRequest(false, "", 0, null, 1, null, null, null, null, null, null, null, null)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV));

        server.verify();
    }

    /**
     * Upcoming movie discover sends tomorrow..+3y and vote_count.gte 0.
     */
    @Test
    void movieDiscoverUpcomingSendsFutureWindow() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        String from = today.plusDays(1).toString();
        String to = today.plusYears(3).toString();
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/movie")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("primary_release_date.gte", from))
                .andExpect(queryParam("primary_release_date.lte", to))
                .andExpect(queryParam("vote_count.gte", "0"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverMovies(new DiscoverRequest(
                        false, "", null, null, 1, null, null, null, "en", null, null, null, true)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE));

        server.verify();
    }

    /**
     * Upcoming TV discover sends tomorrow..+3y on first_air_date.
     */
    @Test
    void tvDiscoverUpcomingSendsFutureWindow() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        String from = today.plusDays(1).toString();
        String to = today.plusYears(3).toString();
        server.expect(requestTo(startsWith("https://api.themoviedb.org/3/discover/tv")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("first_air_date.gte", from))
                .andExpect(queryParam("first_air_date.lte", to))
                .andExpect(queryParam("vote_count.gte", "0"))
                .andRespond(withSuccess(EMPTY_PAGE, MediaType.APPLICATION_JSON));

        client.discoverTvShows(new DiscoverRequest(
                        false, "", null, null, 1, null, null, null, null, null, null, null, true)
                .withDefaults(com.atamanahmet.cinelog.domain.entity.TmdbMediaType.TV));

        server.verify();
    }
}
