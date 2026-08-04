package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ces.service.module.inventory.enums.StockLevel;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import com.ces.service.module.inventory.service.StockClock;
import com.ces.service.module.inventory.service.WarrantyClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Product view. Location and quantity come from the stock rows, never from the product itself. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;
    private UUID branchId;
    private UUID categoryId;
    private String name;
    private String sku;
    private String barcode;
    private String qrCode;
    private String unit;

    /** Sum across every location — the answer to "how many do we have". */
    private BigDecimal totalQuantity;

    /** Every folder holding this product. Empty once the last location is emptied and removed. */
    private List<StockLocationResponse> locations;

    private BigDecimal purchasePrice;
    private Boolean isSerialized;
    private Map<String, Object> attributes;
    private Integer warrantyMonths;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    /**
     * Derived, never stored. Serialized items report {@code NONE} on purpose: their warranty is
     * per-unit, so a single status here would contradict the units underneath it.
     */
    private WarrantyStatus warrantyStatus;
    private BigDecimal minQuantity;
    private BigDecimal criticalQuantity;
    /** Derived from the total against the thresholds — the same rule the badge and filter use. */
    private StockLevel stockLevel;
    /** Who a warranty claim on this product would be addressed to. */
    private String supplier;
    private String notes;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryItemResponse from(
            InventoryItem item, BigDecimal totalQuantity, List<StockLocationResponse> locations) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .branchId(item.getBranchId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .sku(item.getSku())
                .barcode(item.getBarcode())
                .qrCode(item.getQrCode())
                .unit(item.getUnit())
                .totalQuantity(totalQuantity == null ? BigDecimal.ZERO : totalQuantity)
                .locations(locations == null ? List.of() : locations)
                .purchasePrice(item.getPurchasePrice())
                .isSerialized(item.getIsSerialized())
                .attributes(parseAttributes(item.getAttributes()))
                .warrantyMonths(item.getWarrantyMonths())
                .warrantyStartDate(item.getWarrantyStartDate())
                .warrantyEndDate(item.getWarrantyEndDate())
                .warrantyStatus(
                        Boolean.TRUE.equals(item.getIsSerialized())
                                ? WarrantyStatus.NONE
                                : WarrantyClock.statusOf(item.getWarrantyEndDate()))
                .minQuantity(item.getMinQuantity())
                .criticalQuantity(item.getCriticalQuantity())
                .stockLevel(StockClock.levelOf(
                        totalQuantity, item.getMinQuantity(), item.getCriticalQuantity()))
                .supplier(item.getSupplier())
                .notes(item.getNotes())
                .isActive(item.getIsActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private static Map<String, Object> parseAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
