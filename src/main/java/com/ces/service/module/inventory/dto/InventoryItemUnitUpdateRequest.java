package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Edits a unit's status/location/warranty — fields left null are left unchanged. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemUnitUpdateRequest {

    private InventoryUnitStatus status;
    private UUID nodeId;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private String notes;
}
