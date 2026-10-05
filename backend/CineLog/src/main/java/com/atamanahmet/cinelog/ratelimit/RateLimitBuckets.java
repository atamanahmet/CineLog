package com.atamanahmet.cinelog.ratelimit;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

import com.atamanahmet.cinelog.config.RateLimitProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

public final class RateLimitBuckets {

    private RateLimitBuckets() {
    }

    /**
     * Build a greedy-refill Bucket4j bucket from config values.
     */
    public static Bucket create(RateLimitProperties.BucketLimit limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.getCapacity())
                .refillGreedy(limit.getRefillTokens(), Duration.ofSeconds(limit.getRefillDurationSeconds()))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    /**
     * Try one permit with no wait. Returns the probe so callers can read refill delay.
     */
    public static ConsumptionProbe tryAcquire(Bucket bucket) {
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Wait up to timeout for one permit. Zero or negative timeout means fail-fast.
     * When refill needs longer than timeout, parks for the full timeout then retries once.
     */
    public static boolean tryAcquire(Bucket bucket, Duration timeout) throws InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return bucket.tryConsume(1);
        }
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        }
        long maxWaitNanos = timeout.toNanos();
        long nanosToRefill = probe.getNanosToWaitForRefill();
        if (nanosToRefill > maxWaitNanos) {
            parkNanosInterruptibly(maxWaitNanos);
            return bucket.tryConsume(1);
        }
        return bucket.asBlocking().tryConsume(1, timeout);
    }

    /**
     * Park until nanos elapse or the thread is interrupted.
     */
    private static void parkNanosInterruptibly(long nanos) throws InterruptedException {
        long deadline = System.nanoTime() + nanos;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            LockSupport.parkNanos(remaining);
        }
    }
}
