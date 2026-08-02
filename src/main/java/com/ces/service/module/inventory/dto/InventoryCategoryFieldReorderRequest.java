package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** New field order for a category — the position in this list becomes the field's sort_order. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCategoryFieldReorderRequest {

    @NotEmpty
    private List<UUID> fieldIds;
}
