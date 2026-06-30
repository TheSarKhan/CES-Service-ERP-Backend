package com.ces.service.module.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a branch (SRS §5.2).
 */
public record BranchRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) String code,
        @Size(max = 5000) String address,
        @Size(max = 50) String phone,
        Boolean active
) {
}
