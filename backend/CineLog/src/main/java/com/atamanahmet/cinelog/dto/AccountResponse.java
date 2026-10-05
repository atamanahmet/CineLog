package com.atamanahmet.cinelog.dto;

/**
 * Account details shown on the settings page. Email is null until the user sets one.
 */
public record AccountResponse(String username, String email) {

    public AccountResponse {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
