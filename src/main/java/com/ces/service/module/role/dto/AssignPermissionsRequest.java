package com.ces.service.module.role.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload for adding permissions to a role (POST /roles/{id}/permissions). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionsRequest {

    @NotEmpty
    private List<UUID> permissionIds;
}
