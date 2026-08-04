package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * POST /api/v1/auth/refresh request body.
 *
 * <p>{@code branch_id} is optional: when present (and the user is a member of
 * that branch) the refreshed access token stays bound to it, so a silent
 * refresh does not revert the session to the default branch.</p>
 */
public record RefreshRequest(
        @NotBlank @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("branch_id") UUID branchId
) {
}
