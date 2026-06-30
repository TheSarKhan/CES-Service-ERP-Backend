package com.ces.service.module.role.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.role.dto.AssignPermissionsRequest;
import com.ces.service.module.role.dto.PermissionMatrixResponse;
import com.ces.service.module.role.dto.PermissionResponse;
import com.ces.service.module.role.dto.RoleRequest;
import com.ces.service.module.role.dto.RoleResponse;
import com.ces.service.module.role.entity.Permission;
import com.ces.service.module.role.entity.Role;
import com.ces.service.module.role.repository.PermissionRepository;
import com.ces.service.module.role.repository.RoleRepository;
import com.ces.service.module.role.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBAC role management (M16). All reads/writes are branch-scoped via {@link BranchContext#get()}.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>RBAC-V01 — system roles ({@code is_system=true}) cannot be modified destructively / deleted
 *       ({@link ErrorCode#SYSTEM_ROLE_PROTECTED}).</li>
 *   <li>RBAC-V02 — role {@code code} unique within branch ({@link ErrorCode#DUPLICATE_ROLE_CODE}).</li>
 *   <li>RBAC-V03 — a role with active users cannot be deleted
 *       ({@link ErrorCode#ROLE_HAS_ACTIVE_USERS}).</li>
 * </ul>
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        UUID branchId = BranchContext.get();
        return roleRepository
                .findByBranchIdAndDeletedAtIsNull(branchId, pageable)
                .map(role -> RoleResponse.from(role, false));
    }

    @Transactional(readOnly = true)
    public RoleResponse get(UUID id) {
        return RoleResponse.from(loadWithPermissions(id), true);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(UUID id) {
        Role role = loadWithPermissions(id);
        return role.getPermissions().stream()
                .map(PermissionResponse::from)
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .collect(Collectors.toList());
    }

    public RoleResponse create(RoleRequest request) {
        UUID branchId = BranchContext.get();

        // RBAC-V02: code unique within branch.
        if (roleRepository.existsByBranchIdAndCodeAndDeletedAtIsNull(branchId, request.getCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE);
        }

        Role role = Role.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .isSystem(false)
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .permissions(new HashSet<>())
                .build();
        role.setBranchId(branchId);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            role.setPermissions(new HashSet<>(resolvePermissions(request.getPermissionIds())));
        }

        Role saved = roleRepository.save(role);
        return RoleResponse.from(saved, true);
    }

    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = loadWithPermissions(id);

        // RBAC-V01: system roles are protected from edits.
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }

        // RBAC-V02: code unique within branch (only when changed).
        if (!role.getCode().equals(request.getCode())
                && roleRepository.existsByBranchIdAndCodeAndDeletedAtIsNull(
                        role.getBranchId(), request.getCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE);
        }

        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            role.setIsActive(request.getIsActive());
        }
        return RoleResponse.from(role, true);
    }

    public void delete(UUID id) {
        Role role = loadRole(id);

        // RBAC-V01: system roles cannot be deleted.
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }

        // RBAC-V03: role with active users cannot be deleted.
        if (userRoleRepository.existsByRoleId(role.getId())) {
            throw new BusinessException(ErrorCode.ROLE_HAS_ACTIVE_USERS);
        }

        role.setDeletedAt(java.time.Instant.now());
        roleRepository.save(role);
    }

    /** Adds permissions to a role; idempotent for already-present permissions (RBAC-V04 auditable). */
    public RoleResponse addPermissions(UUID id, AssignPermissionsRequest request) {
        Role role = loadWithPermissions(id);
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }
        List<Permission> permissions = resolvePermissions(request.getPermissionIds());
        permissions.forEach(role::addPermission);
        return RoleResponse.from(role, true);
    }

    public RoleResponse removePermission(UUID id, UUID permissionId) {
        Role role = loadWithPermissions(id);
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        return RoleResponse.from(role, true);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions(String module) {
        List<Permission> permissions = (module == null || module.isBlank())
                ? permissionRepository.findAllByOrderByModuleAscCodeAsc()
                : permissionRepository.findByModuleOrderByCodeAsc(module);
        return permissions.stream().map(PermissionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PermissionMatrixResponse permissionMatrix() {
        UUID branchId = BranchContext.get();
        List<PermissionResponse> permissions = permissionRepository
                .findAllByOrderByModuleAscCodeAsc()
                .stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());

        List<Role> roles = roleRepository.findByBranchIdAndDeletedAtIsNull(branchId);

        List<PermissionMatrixResponse.RoleSummary> roleSummaries = roles.stream()
                .map(r -> PermissionMatrixResponse.RoleSummary.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .code(r.getCode())
                        .isSystem(r.getIsSystem())
                        .build())
                .collect(Collectors.toList());

        Map<UUID, List<String>> matrix = roles.stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        r -> r.getPermissions().stream()
                                .map(Permission::getCode)
                                .sorted()
                                .collect(Collectors.toList())));

        return PermissionMatrixResponse.builder()
                .permissions(permissions)
                .roles(roleSummaries)
                .matrix(matrix)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Role loadRole(UUID id) {
        UUID branchId = BranchContext.get();
        return roleRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private Role loadWithPermissions(UUID id) {
        UUID branchId = BranchContext.get();
        return roleRepository
                .findWithPermissionsByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private List<Permission> resolvePermissions(List<UUID> permissionIds) {
        Set<UUID> distinctIds = new HashSet<>(permissionIds);
        List<Permission> found = permissionRepository.findByIdIn(new ArrayList<>(distinctIds));
        if (found.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("One or more permission ids not found");
        }
        return found;
    }
}
