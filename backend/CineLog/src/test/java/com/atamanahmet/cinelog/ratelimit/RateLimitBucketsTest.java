package com.atamanahmet.cinelog.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

class RateLimitBucketsTest {

    /**
     * Fail-fast acquire returns false when the bucket is empty.
     */
    @Test
    void failFastReturnsFalseWhenEmpty() {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofSeconds(60))
                        .build())
                .build();
        assertThat(RateLimitBuckets.tryAcquire(bucket).isConsumed()).isTrue();

        ConsumptionProbe probe = RateLimitBuckets.tryAcquire(bucket);

        assertThat(probe.isConsumed()).isFalse();
        assertThat(probe.getNanosToWaitForRefill()).isPositive();
    }

    /**
     * Waiting acquire succeeds once the bucket refills within the timeout.
     */
    @Test
    void waitingAcquireSucceedsAfterRefill() throws Exception {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofMillis(150))
                        .build())
                .build();
        assertThat(RateLimitBuckets.tryAcquire(bucket).isConsumed()).isTrue();

        boolean acquired = RateLimitBuckets.tryAcquire(bucket, Duration.ofSeconds(2));

        assertThat(acquired).isTrue();
    }

    /**
     * Waiting acquire returns false when refill is slower than the timeout.
     */
    @Test
    void waitingAcquireTimesOutWhenRefillTooSlow() throws Exception {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofSeconds(60))
                        .build())
                .build();
        assertThat(RateLimitBuckets.tryAcquire(bucket).isConsumed()).isTrue();

        long started = System.nanoTime();
        boolean acquired = RateLimitBuckets.tryAcquire(bucket, Duration.ofMillis(100));

        assertThat(acquired).isFalse();
        assertThat(Duration.ofNanos(System.nanoTime() - started).toMillis()).isGreaterThanOrEqualTo(80);
    }

    /**
     * Zero timeout on the waiting API is fail-fast.
     */
    @Test
    void zeroTimeoutIsFailFast() throws Exception {
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(1)
                        .refillGreedy(1, Duration.ofSeconds(60))
                        .build())
                .build();
        assertThat(RateLimitBuckets.tryAcquire(bucket).isConsumed()).isTrue();

        boolean acquired = RateLimitBuckets.tryAcquire(bucket, Duration.ZERO);

        assertThat(acquired).isFalse();
    }
}
