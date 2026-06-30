package com.ces.service.module.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Decoded view of a CES Service JWT (SRS §4.1).
 *
 * @param subject     user UUID ({@code sub})
 * @param email       user email
 * @param branchId    active branch UUID (null on refresh tokens)
 * @param roles       role codes
 * @param permissions permission codes
 * @param jti         unique token id
 * @param issuedAt    issue time
 * @param expiresAt   expiry time
 */
public record JwtClaims(
        UUID subject,
        String email,
        UUID branchId,
        List<String> roles,
        List<String> permissions,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
}
