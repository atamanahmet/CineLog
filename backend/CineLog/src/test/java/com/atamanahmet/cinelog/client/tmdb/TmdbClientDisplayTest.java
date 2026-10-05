package com.atamanahmet.cinelog.client.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

class TmdbClientDisplayTest {

    private static final String API_KEY = "test-tmdb-token-xxxxxxxxxxxxxxxx";
    private static final String MOVIE_URL = "https://api.themoviedb.org/3/movie/99";
    private static final String TV_URL = "https://api.themoviedb.org/3/tv/88";

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
     * Movie display maps id, title, poster, date, overview, vote_average and vote_count.
     */
    @Test
    void movieDisplaySuccess() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 99,
                          "title": "Heat",
                          "poster_path": "/heat.jpg",
                          "release_date": "1995-12-15",
                          "overview": "Crime drama",
                          "vote_average": 7.9,
                          "vote_count": 3210
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getMovieDisplayById(99);

        server.verify();
        assertThat(display).isEqualTo(new MediaDisplay(
                99,
                TmdbMediaType.MOVIE,
                "Heat",
                "/heat.jpg",
                LocalDate.of(1995, 12, 15),
                "Crime drama",
                7.9,
                3210,
                List.of()));
    }

    /**
     * TV display maps name and first_air_date onto title and releaseDate.
     */
    @Test
    void tvDisplaySuccess() {
        server.expect(requestTo(TV_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 88,
                          "name": "The Wire",
                          "poster_path": "/wire.jpg",
                          "first_air_date": "2002-06-02",
                          "overview": "Baltimore"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getTvDisplayById(88);

        server.verify();
        assertThat(display).isEqualTo(new MediaDisplay(
                88,
                TmdbMediaType.TV,
                "The Wire",
                "/wire.jpg",
                LocalDate.of(2002, 6, 2),
                "Baltimore",
                null,
                null,
                List.of()));
        assertThat(display.voteAverage()).isNull();
        assertThat(display.voteCount()).isNull();
    }

    /**
     * Movie genres[].id map onto genreIds.
     */
    @Test
    void movieDisplayMapsGenreIds() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 99,
                          "title": "Heat",
                          "genres": [
                            {"id": 28, "name": "Action"},
                            {"id": 80, "name": "Crime"}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getMovieDisplayById(99);

        server.verify();
        assertThat(display.genreIds()).containsExactly(28, 80);
    }

    /**
     * TV genres[].id map onto genreIds.
     */
    @Test
    void tvDisplayMapsGenreIds() {
        server.expect(requestTo(TV_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 88,
                          "name": "The Wire",
                          "genres": [
                            {"id": 18, "name": "Drama"},
                            {"id": 80, "name": "Crime"}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getTvDisplayById(88);

        server.verify();
        assertThat(display.genreIds()).containsExactly(18, 80);
    }

    /**
     * Missing genres array becomes an empty genreIds list.
     */
    @Test
    void missingGenresGivesEmptyList() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 99,
                          "title": "Heat"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getMovieDisplayById(99);

        server.verify();
        assertThat(display.genreIds()).isEmpty();
    }

    /**
     * Null genres array becomes an empty genreIds list.
     */
    @Test
    void nullGenresGivesEmptyList() {
        server.expect(requestTo(TV_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 88,
                          "name": "The Wire",
                          "genres": null
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getTvDisplayById(88);

        server.verify();
        assertThat(display.genreIds()).isEmpty();
    }

    /**
     * Blank release_date becomes a null LocalDate. Missing vote_average stays null.
     */
    @Test
    void blankReleaseDateGivesNull() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 99,
                          "title": "Heat",
                          "poster_path": "/heat.jpg",
                          "release_date": "",
                          "overview": "Crime drama"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        MediaDisplay display = client.getMovieDisplayById(99);

        server.verify();
        assertThat(display.releaseDate()).isNull();
        assertThat(display.voteAverage()).isNull();
        assertThat(display.title()).isEqualTo("Heat");
    }

    /**
     * TMDB 404 is a TmdbClientException with upstreamStatus 404.
     */
    @Test
    void notFoundIsDistinguishable() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getMovieDisplayById(99))
                .isInstanceOf(TmdbClientException.class)
                .satisfies(ex -> assertThat(((TmdbClientException) ex).getUpstreamStatus()).isEqualTo(404));

        server.verify();
    }

    /**
     * TMDB 5xx is a TmdbClientException with the upstream status.
     */
    @Test
    void serverErrorThrows() {
        server.expect(requestTo(TV_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getTvDisplayById(88))
                .isInstanceOf(TmdbClientException.class)
                .satisfies(ex -> assertThat(((TmdbClientException) ex).getUpstreamStatus()).isEqualTo(500));

        server.verify();
    }

    /**
     * Malformed JSON body fails as a TmdbClientException.
     */
    @Test
    void malformedBodyThrows() {
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getMovieDisplayById(99))
                .isInstanceOf(TmdbClientException.class)
                .satisfies(ex -> assertThat(((TmdbClientException) ex).getUpstreamStatus()).isEqualTo(-1));

        server.verify();
    }
}
