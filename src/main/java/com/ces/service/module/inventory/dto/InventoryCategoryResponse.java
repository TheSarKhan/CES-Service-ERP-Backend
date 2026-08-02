package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Category view. {@code fields} is populated only when the caller asked for detail. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCategoryResponse {

    private UUID id;
    private UUID branchId;
    private String name;
    private String defaultUnit;
    private Boolean isActive;
    private List<InventoryCategoryFieldResponse> fields;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryCategoryResponse from(InventoryCategory category, List<InventoryCategoryFieldResponse> fields) {
        return InventoryCategoryResponse.builder()
                .id(category.getId())
                .branchId(category.getBranchId())
                .name(category.getName())
                .defaultUnit(category.getDefaultUnit())
                .isActive(category.getIsActive())
                .fields(fields)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
