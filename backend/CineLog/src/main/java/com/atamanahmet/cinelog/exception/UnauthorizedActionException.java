package com.atamanahmet.cinelog.exception;

/**
 * The caller is not authenticated for the requested action.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
