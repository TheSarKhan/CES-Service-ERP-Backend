package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/reset-password request body.
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) @JsonProperty("new_password") String newPassword
) {
}
