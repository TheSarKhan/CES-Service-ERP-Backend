package com.ces.service.module.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Role assignment payload (POST /users/{id}/roles). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {

    @NotNull
    private UUID roleId;

    @NotNull
    private UUID branchId;
}
