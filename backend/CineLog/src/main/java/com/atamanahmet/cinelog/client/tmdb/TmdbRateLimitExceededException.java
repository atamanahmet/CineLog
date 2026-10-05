package com.atamanahmet.cinelog.client.tmdb;

/**
 * Thrown when the shared TMDB outbound rate-limit bucket has no tokens left.
 */
public class TmdbRateLimitExceededException extends RuntimeException {

    private final long nanosToWaitForRefill;

    public TmdbRateLimitExceededException(long nanosToWaitForRefill) {
        super("TMDB rate limit exceeded");
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }
}
