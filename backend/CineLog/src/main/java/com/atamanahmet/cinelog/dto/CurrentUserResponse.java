package com.atamanahmet.cinelog.dto;

/**
 * Logged-in username returned by GET /api/auth/me.
 */
public record CurrentUserResponse(String username) {

    public CurrentUserResponse {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
