package com.ces.service.module.auth.service;

import com.ces.service.module.branch.entity.Branch;
import com.ces.service.module.branch.repository.BranchRepository;
import com.ces.service.module.role.repository.UserRoleRepository;
import com.ces.service.module.user.entity.User;
import com.ces.service.module.user.entity.UserBranch;
import com.ces.service.module.user.repository.UserBranchRepository;
import com.ces.service.module.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Real {@link AuthUserGateway} adapter backed by the RBAC/user persistence layer
 * (BACKEND-RBAC agent): {@code UserRepository}, {@code UserBranchRepository},
 * {@code UserRoleRepository}, and {@code BranchRepository}.
 *
 * <p>Brute-force lock policy (SRS §4.5): 5 consecutive failed attempts lock the
 * account for 15 minutes; a successful login resets the counter.</p>
 */
@Component
public class RbacAuthUserGateway implements AuthUserGateway {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;

    public RbacAuthUserGateway(UserRepository userRepository,
                               UserBranchRepository userBranchRepository,
                               UserRoleRepository userRoleRepository,
                               BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.userRoleRepository = userRoleRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).map(this::toAuthUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> findById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId).map(this::toAuthUser);
    }

    @Override
    @Transactional
    public void onLoginSuccess(UUID userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void onLoginFailure(UUID userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId).ifPresent(user -> {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            }
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> resolvePermissions(UUID userId, UUID branchId) {
        return userRoleRepository.findPermissionCodesByUserIdAndBranchId(userId, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> resolveRoles(UUID userId, UUID branchId) {
        return userRoleRepository.findRoleCodesByUserIdAndBranchId(userId, branchId);
    }

    private AuthUser toAuthUser(User user) {
        List<UserBranch> memberships = userBranchRepository.findByUserId(user.getId());
        List<BranchMembership> branches = memberships.stream()
                .map(this::toBranchMembership)
                .toList();

        return new AuthUser(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                user.getLockedUntil(),
                branches
        );
    }

    private BranchMembership toBranchMembership(UserBranch ub) {
        String name = branchRepository.findById(ub.getBranchId())
                .map(Branch::getName)
                .orElse(null);
        return new BranchMembership(
                ub.getBranchId(),
                name,
                Boolean.TRUE.equals(ub.getIsDefault())
        );
    }
}
