package com.ces.service.module.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port that decouples {@link AuthService} from the RBAC/user persistence layer
 * owned by the BACKEND-RBAC agent ({@code com.ces.service.module.user} /
 * {@code ...module.role}).
 *
 * <p>The RBAC agent should provide an adapter ({@code @Component}) backed by
 * {@code UserRepository} / {@code UserRoleRepository} that maps {@code User} +
 * {@code UserRole} → {@code Role} → {@code Permission} into the records below.
 * Defining this port (rather than importing the not-yet-existing RBAC classes)
 * is what keeps this foundation module independently compilable.</p>
 *
 * <p>Assumed {@code User} shape (see task spec): {@code id, branchId, fullName,
 * email, passwordHash, isActive, failedAttempts, lockedUntil, lastLoginAt,
 * getUserBranches()}.</p>
 */
public interface AuthUserGateway {

    /** Load the authentication view of a user by (case-insensitive) email. */
    Optional<AuthUser> findByEmail(String email);

    /** Load the authentication view of a user by id. */
    Optional<AuthUser> findById(UUID userId);

    /** Reset the failed-attempt counter and stamp the successful login. */
    void onLoginSuccess(UUID userId);

    /** Increment the failed-attempt counter; apply lock if the threshold is met. */
    void onLoginFailure(UUID userId);

    /**
     * Resolve the union of permission codes a user holds across all roles, scoped
     * to the active branch.
     */
    List<String> resolvePermissions(UUID userId, UUID branchId);

    /** Resolve the role codes a user holds (scoped to the active branch). */
    List<String> resolveRoles(UUID userId, UUID branchId);

    /**
     * Authentication projection of a user. {@code branches} is the user's full
     * branch membership; the first / default branch is selected on login.
     *
     * @param id           user UUID
     * @param fullName     display name
     * @param email        login email
     * @param passwordHash bcrypt hash for verification
     * @param active       whether the account is active
     * @param lockedUntil  lock expiry, or null when not locked
     * @param branches     branches the user belongs to (id + name + default flag)
     */
    record AuthUser(
            UUID id,
            String fullName,
            String email,
            String passwordHash,
            boolean active,
            Instant lockedUntil,
            List<BranchMembership> branches
    ) {
    }

    /**
     * A user ↔ branch membership row.
     *
     * @param branchId   branch UUID
     * @param branchName branch display name
     * @param isDefault  whether this is the user's default branch
     */
    record BranchMembership(
            UUID branchId,
            String branchName,
            boolean isDefault
    ) {
    }
}
