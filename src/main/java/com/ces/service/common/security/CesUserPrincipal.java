package com.ces.service.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated principal placed in the Spring {@code SecurityContext} by the
 * {@code JwtAuthenticationFilter}. Carries the resolved identity, the active branch,
 * the set of branches the user may operate in, and the role/permission claims.
 *
 * @param userId      user UUID (JWT {@code sub})
 * @param email       user email (JWT {@code email})
 * @param branchId    currently active branch UUID (JWT {@code branch_id})
 * @param branches    every branch the user belongs to
 * @param roles       role codes (e.g. SERVICE_MANAGER)
 * @param permissions permission codes (granted authorities)
 */
public record CesUserPrincipal(
        UUID userId,
        String email,
        UUID branchId,
        List<UUID> branches,
        List<String> roles,
        List<String> permissions
) {

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean canAccessBranch(UUID candidate) {
        return branches != null && branches.contains(candidate);
    }
}
