package com.atamanahmet.cinelog.ratelimit;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.atamanahmet.cinelog.client.tmdb.TmdbRateLimitExceededException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceptionHandler {

    @ExceptionHandler(TmdbRateLimitExceededException.class)
    public ResponseEntity<Void> handleTmdbRateLimit(TmdbRateLimitExceededException ex) {
        return RateLimitResponses.tooManyRequests(ex.getNanosToWaitForRefill());
    }
}
