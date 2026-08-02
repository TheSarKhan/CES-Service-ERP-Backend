package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryCategory;
import com.ces.service.module.inventory.entity.InventoryNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Layer node view. {@code hasChildren} is computed by the service (not persisted). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryNodeResponse {

    private UUID id;
    private UUID branchId;
    private UUID parentId;
    private String name;
    private String code;
    private String qrCode;
    private String barcode;
    private Boolean isActive;
    private String notes;
    private Boolean hasChildren;
    /** Categories allowed at this node; empty = unrestricted. */
    private List<UUID> categoryIds;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryNodeResponse from(InventoryNode node, boolean hasChildren) {
        return InventoryNodeResponse.builder()
                .id(node.getId())
                .branchId(node.getBranchId())
                .parentId(node.getParentId())
                .name(node.getName())
                .code(node.getCode())
                .qrCode(node.getQrCode())
                .barcode(node.getBarcode())
                .isActive(node.getIsActive())
                .notes(node.getNotes())
                .hasChildren(hasChildren)
                .categoryIds(node.getAllowedCategories().stream()
                        .map(InventoryCategory::getId)
                        .sorted()
                        .collect(Collectors.toList()))
                .createdAt(node.getCreatedAt())
                .updatedAt(node.getUpdatedAt())
                .build();
    }
}
