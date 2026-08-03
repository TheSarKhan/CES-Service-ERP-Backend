package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import com.ces.service.module.inventory.service.WarrantyClock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Serialized unit view, with a computed (not persisted) warranty status for search/UI. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemUnitResponse {

    private UUID id;
    private UUID branchId;
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private UUID nodeId;
    private String serialNumber;
    private String qrCode;
    private String barcode;
    private InventoryUnitStatus status;
    private LocalDate purchaseDate;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private WarrantyStatus warrantyStatus;
    private Instant failedAt;
    private String failureNotes;
    private UUID usedInWorkOrderId;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryItemUnitResponse from(InventoryItemUnit unit, String itemName, String itemSku) {
        return InventoryItemUnitResponse.builder()
                .id(unit.getId())
                .branchId(unit.getBranchId())
                .itemId(unit.getItemId())
                .itemName(itemName)
                .itemSku(itemSku)
                .nodeId(unit.getNodeId())
                .serialNumber(unit.getSerialNumber())
                .qrCode(unit.getQrCode())
                .barcode(unit.getBarcode())
                .status(unit.getStatus())
                .purchaseDate(unit.getPurchaseDate())
                .warrantyStartDate(unit.getWarrantyStartDate())
                .warrantyEndDate(unit.getWarrantyEndDate())
                .warrantyStatus(WarrantyClock.statusOf(unit.getWarrantyEndDate()))
                .failedAt(unit.getFailedAt())
                .failureNotes(unit.getFailureNotes())
                .usedInWorkOrderId(unit.getUsedInWorkOrderId())
                .notes(unit.getNotes())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
}
