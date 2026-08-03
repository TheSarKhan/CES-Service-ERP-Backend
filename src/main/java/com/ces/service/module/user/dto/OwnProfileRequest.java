package com.ces.service.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * What a user may change about themselves. Deliberately narrow: email, roles, branch and active
 * state stay with the administrator, so nobody can widen their own access from here.
 */
@Getter
@Setter
public class OwnProfileRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 50)
    private String phone;

    @Size(max = 100)
    private String position;
}
