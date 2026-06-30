package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST /api/v1/auth/login | /refresh | /switch-branch response body (SRS §4.2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        AuthUserDto user
) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds, AuthUserDto user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
