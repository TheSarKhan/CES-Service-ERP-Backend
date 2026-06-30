package com.ces.service.module.user.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.role.entity.Role;
import com.ces.service.module.role.entity.UserRole;
import com.ces.service.module.role.entity.UserRoleId;
import com.ces.service.module.role.repository.RoleRepository;
import com.ces.service.module.role.repository.UserRoleRepository;
import com.ces.service.module.user.dto.AssignRoleRequest;
import com.ces.service.module.user.dto.ResetPasswordRequest;
import com.ces.service.module.user.dto.UserActivityResponse;
import com.ces.service.module.user.dto.UserDetailResponse;
import com.ces.service.module.user.dto.UserRequest;
import com.ces.service.module.user.dto.UserResponse;
import com.ces.service.module.user.entity.User;
import com.ces.service.module.user.entity.UserBranch;
import com.ces.service.module.user.entity.UserBranchId;
import com.ces.service.module.user.repository.UserBranchRepository;
import com.ces.service.module.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User administration (M15). Branch-scoped reads via {@link BranchContext#get()}.
 *
 * <p>Business rules:
 * <ul>
 *   <li>USR-V01 — email globally unique → {@link ErrorCode#DUPLICATE_EMAIL}.</li>
 *   <li>USR-V02 — password min 8 chars, &ge;1 uppercase, &ge;1 digit → {@link ErrorCode#WEAK_PASSWORD}.</li>
 *   <li>USR-V03 — cannot deactivate own account → {@link ErrorCode#CANNOT_DEACTIVATE_SELF}.</li>
 *   <li>USR-V04 — user with active Work Orders cannot be deleted → {@link ErrorCode#USER_HAS_ACTIVE_WO}
 *       (WO module not yet built — guard stubbed, see {@link #hasActiveWorkOrders(UUID)}).</li>
 *   <li>USR-V06 — assigned role's branch must be one of the user's branches.</li>
 *   <li>USR-V08 — the last ADMIN cannot lose the admin role / be deactivated → {@link ErrorCode#LAST_ADMIN}.</li>
 * </ul>
 */
@Service
@Transactional
public class UserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserBranchRepository userBranchRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Boolean isActive, Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<User> page = (isActive == null)
                ? userRepository.findByBranchIdAndDeletedAtIsNull(branchId, pageable)
                : userRepository.findByBranchIdAndIsActiveAndDeletedAtIsNull(branchId, isActive, pageable);
        return page.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse get(UUID id) {
        User user = loadUser(id);
        return toDetail(user);
    }

    public UserResponse create(UserRequest request) {
        UUID branchId = BranchContext.get();

        // USR-V01: email unique system-wide.
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // USR-V02: password strength.
        validatePassword(request.getPassword());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .position(request.getPosition())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .failedAttempts(0)
                .build();
        UUID primaryBranch = request.getBranchId() == null ? branchId : request.getBranchId();
        user.setBranchId(primaryBranch);
        user = userRepository.save(user);

        // Primary branch membership (default).
        addBranchMembership(user.getId(), primaryBranch, true);

        // Optional initial role assignments.
        if (request.getRoleIds() != null) {
            for (UserRequest.RoleAssignment ra : request.getRoleIds()) {
                assignRoleInternal(user.getId(), ra.getRoleId(), ra.getBranchId());
            }
        }
        return UserResponse.from(user);
    }

    public UserResponse update(UUID id, UserRequest request) {
        User user = loadUser(id);

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        return UserResponse.from(user);
    }

    public UserResponse activate(UUID id) {
        User user = loadUser(id);
        user.setIsActive(true);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        return UserResponse.from(user);
    }

    public UserResponse deactivate(UUID id) {
        User user = loadUser(id);

        // USR-V03: cannot deactivate own account.
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId != null && currentUserId.equals(user.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_DEACTIVATE_SELF);
        }

        // USR-V08: cannot deactivate the last active admin in the branch.
        if (isLastAdmin(user)) {
            throw new BusinessException(ErrorCode.LAST_ADMIN);
        }

        user.setIsActive(false);
        return UserResponse.from(user);
    }

    public void delete(UUID id) {
        User user = loadUser(id);

        // USR-V04: user with active Work Orders cannot be deleted.
        if (hasActiveWorkOrders(user.getId())) {
            throw new BusinessException(ErrorCode.USER_HAS_ACTIVE_WO);
        }
        // USR-V08: protect the last admin.
        if (isLastAdmin(user)) {
            throw new BusinessException(ErrorCode.LAST_ADMIN);
        }

        // USR-V05: soft delete — created records are preserved (history intact).
        user.setDeletedAt(Instant.now());
        user.setIsActive(false);
        userRepository.save(user);
    }

    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = loadUser(id);
        validatePassword(request.getNewPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public void assignRole(UUID id, AssignRoleRequest request) {
        loadUser(id);
        assignRoleInternal(id, request.getRoleId(), request.getBranchId());
    }

    public void revokeRole(UUID id, UUID roleId, UUID branchId) {
        User user = loadUser(id);

        // USR-V08: an admin cannot lose the last admin role (system needs &ge;1 admin).
        Role role = roleRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(roleId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        if (ADMIN_ROLE_CODE.equals(role.getCode())
                && userRoleRepository.countActiveUsersByRoleCodeAndBranchId(ADMIN_ROLE_CODE, branchId) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN);
        }

        userRoleRepository.deleteByUserIdAndRoleIdAndBranchId(user.getId(), roleId, branchId);
    }

    public void addBranch(UUID id, UUID branchId) {
        loadUser(id);
        boolean firstMembership = userBranchRepository.countByUserId(id) == 0;
        addBranchMembership(id, branchId, firstMembership);
    }

    public void removeBranch(UUID id, UUID branchId) {
        loadUser(id);
        userBranchRepository.deleteByUserIdAndBranchId(id, branchId);
    }

    @Transactional(readOnly = true)
    public UserActivityResponse activity(UUID id) {
        User user = loadUser(id);
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        return UserActivityResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .lastLoginAt(user.getLastLoginAt())
                .failedAttempts(user.getFailedAttempts())
                .lockedUntil(user.getLockedUntil())
                .isLocked(locked)
                .isActive(user.getIsActive())
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void assignRoleInternal(UUID userId, UUID roleId, UUID branchId) {
        // USR-V06: role's branch must be one of the user's branches.
        if (!userBranchRepository.existsByUserIdAndBranchId(userId, branchId)) {
            throw new BusinessException(ErrorCode.ROLE_BRANCH_MISMATCH);
        }
        roleRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(roleId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        if (userRoleRepository.existsByUserIdAndRoleIdAndBranchId(userId, roleId, branchId)) {
            return; // idempotent
        }
        UserRole userRole = UserRole.builder()
                .id(UserRoleId.builder().userId(userId).roleId(roleId).branchId(branchId).build())
                .build();
        userRoleRepository.save(userRole);
    }

    private void addBranchMembership(UUID userId, UUID branchId, boolean isDefault) {
        if (userBranchRepository.existsByUserIdAndBranchId(userId, branchId)) {
            return;
        }
        UserBranch membership = UserBranch.builder()
                .id(UserBranchId.builder().userId(userId).branchId(branchId).build())
                .isDefault(isDefault)
                .build();
        userBranchRepository.save(membership);
    }

    private boolean isLastAdmin(User user) {
        List<UserRole> roles = userRoleRepository.findByUserId(user.getId());
        for (UserRole ur : roles) {
            Role role = ur.getRole();
            if (role != null && ADMIN_ROLE_CODE.equals(role.getCode())) {
                long activeAdmins = userRoleRepository.countActiveUsersByRoleCodeAndBranchId(
                        ADMIN_ROLE_CODE, ur.getBranchId());
                if (activeAdmins <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * USR-V04 guard stub. The Work Order module (M06) is not yet built; once available this should
     * query for active (non-closed/cancelled) work orders assigned to / created by the user.
     */
    private boolean hasActiveWorkOrders(UUID userId) {
        // TODO(M06): integrate WorkOrderRepository.existsActiveByAssignee(userId) when available.
        return false;
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
    }

    private User loadUser(UUID id) {
        UUID branchId = BranchContext.get();
        return userRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserDetailResponse toDetail(User user) {
        List<UserDetailResponse.BranchMembership> branches = new ArrayList<>();
        for (UserBranch ub : userBranchRepository.findByUserId(user.getId())) {
            branches.add(UserDetailResponse.BranchMembership.builder()
                    .branchId(ub.getBranchId())
                    .isDefault(ub.getIsDefault())
                    .build());
        }
        List<UserDetailResponse.RoleAssignmentView> roles = new ArrayList<>();
        for (UserRole ur : userRoleRepository.findByUserId(user.getId())) {
            Role role = ur.getRole();
            roles.add(UserDetailResponse.RoleAssignmentView.builder()
                    .roleId(ur.getRoleId())
                    .roleName(role == null ? null : role.getName())
                    .roleCode(role == null ? null : role.getCode())
                    .branchId(ur.getBranchId())
                    .build());
        }
        return UserDetailResponse.builder()
                .id(user.getId())
                .branchId(user.getBranchId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .position(user.getPosition())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .failedAttempts(user.getFailedAttempts())
                .lockedUntil(user.getLockedUntil())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .branches(branches)
                .roles(roles)
                .build();
    }
}
