package com.atamanahmet.cinelog.client;

/**
 * The recommendation engine could not be reached or did not return a valid answer.
 */
public class RecommendationUnavailableException extends RuntimeException {

    public RecommendationUnavailableException(String message) {
        super(message);
    }

    public RecommendationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
