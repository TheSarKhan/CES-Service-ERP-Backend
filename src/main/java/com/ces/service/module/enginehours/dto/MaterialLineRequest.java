package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.enums.MaterialKind;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One material used during a maintenance completion. Which fields are required depends on
 * {@code kind} — validated in {@code VehicleMaintenanceCompletionService}, not here, so a bad line
 * can be reported by index rather than a generic Jakarta constraint message.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialLineRequest {

    @NotNull
    private MaterialKind kind;

    /** CONSUMABLE only. */
    private UUID inventoryItemId;
    private UUID inventoryNodeId;
    private BigDecimal quantity;

    /** SERIALIZED only. */
    private UUID inventoryUnitId;

    private String notes;
}
