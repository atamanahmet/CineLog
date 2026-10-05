package com.atamanahmet.cinelog.exception;

/**
 * Another account already uses the requested email.
 */
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("Email is already in use");
    }
}
