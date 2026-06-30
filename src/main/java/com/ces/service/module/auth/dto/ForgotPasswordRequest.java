package com.ces.service.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/forgot-password request body.
 */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}
