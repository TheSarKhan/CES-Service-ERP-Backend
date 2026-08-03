package com.ces.service.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Admin-initiated password reset (PATCH /users/{id}/reset-password). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    /**
     * Optional. Left empty, the server generates a temporary password and returns it once — that
     * is the normal path, so the admin never has to invent one.
     */
    @Size(min = 8, max = 100)
    private String newPassword;
}
