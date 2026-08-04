package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryLot;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One batch, with how close it is to expiring. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLotResponse {

    private UUID id;
    private UUID itemId;
    private String itemName;
    private UUID nodeId;
    private String nodeName;
    private String lotNumber;
    private BigDecimal quantity;
    private LocalDate expiryDate;
    private LocalDate receivedDate;
    private String notes;

    /**
     * Reuses the warranty vocabulary on purpose: "running out of time" is the same idea and the UI
     * already speaks it. NONE means the batch has no expiry date at all.
     */
    private WarrantyStatus expiryStatus;

    /** Negative once expired; null when the batch never expires. */
    private Long daysRemaining;

    public static InventoryLotResponse from(
            InventoryLot lot, String itemName, String nodeName, LocalDate today, int warningDays) {
        LocalDate expiry = lot.getExpiryDate();
        WarrantyStatus status;
        if (expiry == null) {
            status = WarrantyStatus.NONE;
        } else if (expiry.isBefore(today)) {
            status = WarrantyStatus.EXPIRED;
        } else if (!expiry.isAfter(today.plusDays(warningDays))) {
            status = WarrantyStatus.EXPIRING_SOON;
        } else {
            status = WarrantyStatus.ACTIVE;
        }

        return InventoryLotResponse.builder()
                .id(lot.getId())
                .itemId(lot.getItemId())
                .itemName(itemName)
                .nodeId(lot.getNodeId())
                .nodeName(nodeName)
                .lotNumber(lot.getLotNumber())
                .quantity(lot.getQuantity())
                .expiryDate(expiry)
                .receivedDate(lot.getReceivedDate())
                .notes(lot.getNotes())
                .expiryStatus(status)
                .daysRemaining(expiry == null ? null : expiry.toEpochDay() - today.toEpochDay())
                .build();
    }
}
