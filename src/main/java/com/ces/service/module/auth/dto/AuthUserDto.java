package com.ces.service.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * User payload embedded in the login response (SRS §4.2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthUserDto(
        UUID id,
        @JsonProperty("full_name") String fullName,
        String email,
        @JsonProperty("branch_id") UUID branchId,
        List<BranchSummary> branches,
        List<String> roles,
        List<String> permissions
) {

    /**
     * Minimal branch descriptor for the user's branch list.
     */
    public record BranchSummary(UUID id, String name) {
    }
}
