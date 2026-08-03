package com.ces.service.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** "Parolu dəyiş" — the signed-in user replacing their own password. */
@Getter
@Setter
public class ChangeOwnPasswordRequest {

    /**
     * The password currently in use. Required even right after an admin reset: without it, a
     * hijacked session could set a new password without knowing the old one.
     */
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(max = 255)
    private String newPassword;
}
