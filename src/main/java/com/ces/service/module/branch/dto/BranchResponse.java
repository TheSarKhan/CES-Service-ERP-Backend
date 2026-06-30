package com.ces.service.module.branch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Branch response view.
 */
public record BranchResponse(
        UUID id,
        String name,
        String code,
        String address,
        String phone,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
