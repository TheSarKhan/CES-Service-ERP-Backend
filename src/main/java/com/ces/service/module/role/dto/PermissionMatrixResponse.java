package com.ces.service.module.role.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Full Role &times; Permission matrix (GET /permissions/matrix).
 *
 * <p>{@code permissions} is the catalog (rows). {@code roles} lists the branch roles (columns).
 * {@code matrix} maps {@code roleId -> set of permission codes} that the role grants, so the UI can
 * render a checkbox grid.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionMatrixResponse {

    private List<PermissionResponse> permissions;
    private List<RoleSummary> roles;
    private Map<UUID, List<String>> matrix;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleSummary {
        private UUID id;
        private String name;
        private String code;
        private Boolean isSystem;
    }
}
