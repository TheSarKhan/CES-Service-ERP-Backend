package com.ces.service.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Convenience accessors for the currently authenticated {@link CesUserPrincipal}.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CesUserPrincipal> getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CesUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentPrincipal().map(CesUserPrincipal::userId);
    }

    public static Optional<String> getCurrentEmail() {
        return getCurrentPrincipal().map(CesUserPrincipal::email);
    }

    public static Optional<UUID> getCurrentBranchId() {
        return getCurrentPrincipal().map(CesUserPrincipal::branchId);
    }
}
