package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Create / update / move payload for a Layer node. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryNodeRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String code;

    /** Parent node id; null = root. */
    private UUID parentId;

    @Size(max = 2000)
    private String notes;

    private Boolean isActive;

    /**
     * Categories allowed at this node; null = leave unchanged, empty list = clear the
     * restriction (unrestricted), non-empty = replace the full set.
     */
    private List<UUID> categoryIds;
}
