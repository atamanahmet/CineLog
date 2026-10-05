package com.atamanahmet.cinelog.client.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

class TmdbClientRateLimitTest {

    private static final String API_KEY = "test-tmdb-token-xxxxxxxxxxxxxxxx";
    private static final String MOVIE_URL = "https://api.themoviedb.org/3/movie/1";
    private static final String BODY = """
            {"id":1,"title":"One","poster_path":"/a.jpg","release_date":"1990-01-01","overview":"o"}
            """;
    private static final Duration RETRY_AFTER_DEFAULT = Duration.ofSeconds(1);

    /**
     * Interactive client waits up to 1s and succeeds when a permit refills.
     */
    @Test
    void interactiveWaitsUpToOneSecondAndSucceeds() {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofMillis(150))
                        .build())
                .build();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = interactive(builder, bucket, Duration.ofSeconds(1));

        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        assertThat(client.getMovieDisplayById(1).title()).isEqualTo("One");
        assertThat(client.getMovieDisplayById(1).title()).isEqualTo("One");
        server.verify();
    }

    /**
     * Interactive client throws after the configured wait when the bucket does not refill.
     */
    @Test
    void interactiveThrowsAfterOneSecondWhenNoRefill() {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofSeconds(60))
                        .build())
                .build();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = interactive(builder, bucket, Duration.ofSeconds(1));

        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        assertThat(client.getMovieDisplayById(1).title()).isEqualTo("One");

        long started = System.nanoTime();
        assertThatThrownBy(() -> client.getMovieDisplayById(1))
                .isInstanceOf(TmdbRateLimitExceededException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - started).toMillis()).isGreaterThanOrEqualTo(900);
        server.verify();
    }

    /**
     * Interrupted permit wait exits cleanly and restores the interrupt flag.
     */
    @Test
    void interruptedPermitWaitExitsCleanly() throws Exception {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofSeconds(60))
                        .build())
                .build();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = interactive(builder, bucket, Duration.ofSeconds(30));

        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        assertThat(client.getMovieDisplayById(1).title()).isEqualTo("One");

        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                client.getMovieDisplayById(1);
            } catch (Throwable t) {
                error.set(t);
            }
        });
        waiter.start();
        Thread.sleep(50);
        waiter.interrupt();
        waiter.join(2000);

        assertThat(waiter.isAlive()).isFalse();
        assertThat(error.get())
                .isInstanceOf(TmdbClientException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(InterruptedException.class);
        server.verify();
    }

    /**
     * Hydrate path retries once on upstream 429 when the deadline allows Retry-After.
     */
    @Test
    void upstream429WithEnoughDeadlineRetriesOnce() {
        Bucket bucket = unlimitedBucket();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = hydrate(builder, bucket, Duration.ofSeconds(5));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        Instant deadline = Instant.now().plusSeconds(5);
        assertThat(client.getMovieDisplayById(1, deadline).title()).isEqualTo("One");
        server.verify();
    }

    /**
     * Hydrate path skips without retry when the deadline cannot cover Retry-After.
     */
    @Test
    void upstream429WithNotEnoughDeadlineSkipsWithoutRetry() {
        Bucket bucket = unlimitedBucket();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = hydrate(builder, bucket, Duration.ofSeconds(5));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

        Instant deadline = Instant.now().plusMillis(200);
        assertThatThrownBy(() -> client.getMovieDisplayById(1, deadline))
                .isInstanceOf(TmdbClientException.class)
                .satisfies(ex -> assertThat(((TmdbClientException) ex).getUpstreamStatus()).isEqualTo(429));
        server.verify();
    }

    /**
     * Interactive callers surface a real 429 without retrying.
     */
    @Test
    void interactiveUpstream429DoesNotRetry() {
        Bucket bucket = unlimitedBucket();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TmdbClient client = interactive(builder, bucket, Duration.ofSeconds(1));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");
        server.expect(requestTo(MOVIE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

        assertThatThrownBy(() -> client.getMovieDisplayById(1))
                .isInstanceOf(TmdbClientException.class)
                .satisfies(ex -> assertThat(((TmdbClientException) ex).getUpstreamStatus()).isEqualTo(429));
        server.verify();
    }

    /**
     * Build a client with interactive settings.
     */
    private static TmdbClient interactive(RestClient.Builder builder, Bucket bucket, Duration maxWait) {
        return new TmdbClient(API_KEY, builder, bucket, maxWait, false, RETRY_AFTER_DEFAULT);
    }

    /**
     * Build a client with hydrate 429-retry settings.
     */
    private static TmdbClient hydrate(RestClient.Builder builder, Bucket bucket, Duration maxWait) {
        return new TmdbClient(API_KEY, builder, bucket, maxWait, true, RETRY_AFTER_DEFAULT);
    }

    /**
     * Build a bucket that never blocks tests on local permits.
     */
    private static Bucket unlimitedBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1000)
                        .refillGreedy(1000, Duration.ofSeconds(1))
                        .build())
                .build();
    }
}
