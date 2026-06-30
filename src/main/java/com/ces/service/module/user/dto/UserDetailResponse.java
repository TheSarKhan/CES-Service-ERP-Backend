package com.ces.service.module.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Full user detail including branch memberships and per-branch role assignments (GET /users/{id}). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    private UUID id;
    private UUID branchId;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Integer failedAttempts;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BranchMembership> branches;
    private List<RoleAssignmentView> roles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchMembership {
        private UUID branchId;
        private Boolean isDefault;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleAssignmentView {
        private UUID roleId;
        private String roleName;
        private String roleCode;
        private UUID branchId;
    }
}
