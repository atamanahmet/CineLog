package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

/**
 * Current password for confirmation plus the new password, checked by the same rules as register.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword String newPassword) {
}
