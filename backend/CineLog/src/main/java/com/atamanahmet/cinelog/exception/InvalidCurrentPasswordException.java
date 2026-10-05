package com.atamanahmet.cinelog.exception;

/**
 * The current password sent to confirm an account change does not match.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
