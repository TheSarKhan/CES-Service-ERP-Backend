package com.ces.service.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create / update payload for a user (M15.2).
 *
 * <p>{@code password} is only consumed on create; on update it is ignored (password changes go
 * through the dedicated reset endpoint). {@code branchId} is the user's primary branch. Optional
 * {@code roleIds} performs an initial role assignment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    /** Required on create; ignored on update. Strength is validated server-side (USR-V02). */
    private String password;

    @Size(max = 50)
    private String phone;

    @Size(max = 100)
    private String position;

    @NotNull
    private UUID branchId;

    private Boolean isActive;

    /** Optional initial role assignment. */
    private List<RoleAssignment> roleIds;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleAssignment {
        @NotNull
        private UUID roleId;

        @NotNull
        private UUID branchId;
    }
}
