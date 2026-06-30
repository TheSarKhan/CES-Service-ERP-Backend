package com.ces.service.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/login request body (SRS §4.2).
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
