package com.atamanahmet.cinelog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New email plus the current password as confirmation.
 */
public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String currentPassword) {
}
