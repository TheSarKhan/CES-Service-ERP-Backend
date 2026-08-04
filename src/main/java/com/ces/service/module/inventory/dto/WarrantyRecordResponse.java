package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.enums.WarrantyRecordType;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import com.ces.service.module.inventory.repository.WarrantyRecordRow;
import com.ces.service.module.inventory.service.WarrantyClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One warranty search result — a serialized unit or a whole non-serialized product, in the same
 * shape so the UI can render them in one list.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyRecordResponse {

    /** Unit id for a UNIT row, item id for an ITEM row — what the UI opens on click. */
    private UUID recordId;

    private WarrantyRecordType recordType;
    private UUID itemId;
    private String itemName;
    private String itemSku;

    /** Null on ITEM rows. */
    private String serialNumber;

    /** Null on ITEM rows — a product has no lifecycle status, only its units do. */
    private InventoryUnitStatus unitStatus;

    private UUID nodeId;
    private String barcode;
    private String qrCode;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;

    /** Derived from the end date via {@link WarrantyClock} — the same rule the badge uses. */
    private WarrantyStatus warrantyStatus;

    /** Remaining days; negative once expired, null when there is no end date. */
    private Long daysRemaining;

    private String supplier;

    /** Stock on hand; null on UNIT rows. */
    private BigDecimal quantity;

    private String unit;

    /** The most recent claim filed for this record, if any — null means nobody has chased it. */
    private WarrantyClaimResponse latestClaim;

    public static WarrantyRecordResponse from(
            WarrantyRecordRow row, LocalDate today, WarrantyClaimResponse latestClaim) {
        LocalDate end = row.getWarrantyEndDate();
        return WarrantyRecordResponse.builder()
                .recordId(row.getRecordId())
                .recordType(WarrantyRecordType.valueOf(row.getRecordType()))
                .itemId(row.getItemId())
                .itemName(row.getItemName())
                .itemSku(row.getItemSku())
                .serialNumber(row.getSerialNumber())
                .unitStatus(
                        row.getUnitStatus() == null
                                ? null
                                : InventoryUnitStatus.valueOf(row.getUnitStatus()))
                .nodeId(row.getNodeId())
                .barcode(row.getBarcode())
                .qrCode(row.getQrCode())
                .warrantyStartDate(row.getWarrantyStartDate())
                .warrantyEndDate(end)
                .warrantyStatus(WarrantyClock.statusOf(end, today))
                .daysRemaining(end == null ? null : end.toEpochDay() - today.toEpochDay())
                .supplier(row.getSupplier())
                .quantity(row.getQuantity())
                .unit(row.getUnit())
                .latestClaim(latestClaim)
                .build();
    }
}
