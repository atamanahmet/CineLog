package com.atamanahmet.cinelog.exception;

/**
 * No current user is available from the security context.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found");
    }
}
