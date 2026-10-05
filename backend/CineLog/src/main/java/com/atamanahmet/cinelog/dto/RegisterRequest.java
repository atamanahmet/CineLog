package com.atamanahmet.cinelog.dto;

import com.atamanahmet.cinelog.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @StrongPassword String password) {
}
