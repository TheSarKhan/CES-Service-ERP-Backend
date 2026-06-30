package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/refresh request body.
 */
public record RefreshRequest(
        @NotBlank @JsonProperty("refresh_token") String refreshToken
) {
}
