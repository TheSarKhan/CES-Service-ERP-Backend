package com.ces.service.module.role.dto;

import com.ces.service.module.role.entity.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Role detail, including its assigned permissions. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private UUID id;
    private UUID branchId;
    private String name;
    private String code;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private List<PermissionResponse> permissions;
    private Instant createdAt;
    private Instant updatedAt;

    public static RoleResponse from(Role role, boolean includePermissions) {
        List<PermissionResponse> perms = null;
        if (includePermissions && role.getPermissions() != null) {
            perms = role.getPermissions().stream()
                    .map(PermissionResponse::from)
                    .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                    .collect(Collectors.toList());
        }
        return RoleResponse.builder()
                .id(role.getId())
                .branchId(role.getBranchId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .isActive(role.getIsActive())
                .permissions(perms)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
