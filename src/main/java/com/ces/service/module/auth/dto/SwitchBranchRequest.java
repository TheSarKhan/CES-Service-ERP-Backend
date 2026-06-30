package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * POST /api/v1/auth/switch-branch request body — re-issues tokens scoped to the
 * requested branch (SRS §4.1, §5.3).
 */
public record SwitchBranchRequest(
        @NotNull @JsonProperty("branch_id") UUID branchId
) {
}
