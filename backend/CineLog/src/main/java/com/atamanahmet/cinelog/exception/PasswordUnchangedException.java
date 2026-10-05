package com.atamanahmet.cinelog.exception;

/**
 * The new password equals the current one.
 */
public class PasswordUnchangedException extends RuntimeException {

    public PasswordUnchangedException() {
        super("New password must be different from the current password");
    }
}
