package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletionMaterial;
import com.ces.service.module.enginehours.enums.MaterialKind;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialLineResponse {

    private UUID id;
    private MaterialKind kind;
    private UUID inventoryItemId;
    private String inventoryItemName;
    private UUID inventoryNodeId;
    private BigDecimal quantity;
    private String unit;
    /** Non-null once a stock-out has been submitted for this line — see it decided in Təsdiqləmələr. */
    private UUID stockApprovalRequestId;
    private UUID inventoryUnitId;
    private String serialNumber;
    private UUID vehicleComponentId;
    private String notes;

    public static MaterialLineResponse from(VehicleMaintenanceCompletionMaterial m) {
        return MaterialLineResponse.builder()
                .id(m.getId())
                .kind(m.getKind())
                .inventoryItemId(m.getInventoryItemId())
                .inventoryItemName(m.getInventoryItemName())
                .inventoryNodeId(m.getInventoryNodeId())
                .quantity(m.getQuantity())
                .unit(m.getUnit())
                .stockApprovalRequestId(m.getStockApprovalRequestId())
                .inventoryUnitId(m.getInventoryUnitId())
                .serialNumber(m.getSerialNumber())
                .vehicleComponentId(m.getVehicleComponentId())
                .notes(m.getNotes())
                .build();
    }
}
