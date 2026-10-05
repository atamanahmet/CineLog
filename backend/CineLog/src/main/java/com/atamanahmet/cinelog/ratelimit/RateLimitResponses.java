package com.atamanahmet.cinelog.ratelimit;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;

public final class RateLimitResponses {

    private RateLimitResponses() {
    }

    /**
     * Write 429 with Retry-After seconds until next token (ceil of nanos).
     */
    public static void writeTooManyRequests(HttpServletResponse response, long nanosToWaitForRefill)
            throws java.io.IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(nanosToWaitForRefill)));
    }

    /**
     * Build empty 429 ResponseEntity with Retry-After header.
     */
    public static ResponseEntity<Void> tooManyRequests(long nanosToWaitForRefill) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(nanosToWaitForRefill)))
                .build();
    }

    static long retryAfterSeconds(long nanosToWaitForRefill) {
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanosToWaitForRefill);
        if (nanosToWaitForRefill % 1_000_000_000L != 0L) {
            seconds++;
        }
        return Math.max(1L, seconds);
    }
}
