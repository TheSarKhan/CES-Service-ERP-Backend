package com.ces.service.module.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Create / update payload for a role (M16.4). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String code;

    @Size(max = 2000)
    private String description;

    private Boolean isActive;

    /** Optional initial permission assignment on create. */
    private List<UUID> permissionIds;
}
