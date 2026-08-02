package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Product view. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;
    private UUID branchId;
    private UUID nodeId;
    private UUID categoryId;
    private String name;
    private String sku;
    private String barcode;
    private String qrCode;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private Boolean isSerialized;
    private Map<String, Object> attributes;
    private String notes;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryItemResponse from(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .branchId(item.getBranchId())
                .nodeId(item.getNodeId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .sku(item.getSku())
                .barcode(item.getBarcode())
                .qrCode(item.getQrCode())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .purchasePrice(item.getPurchasePrice())
                .isSerialized(item.getIsSerialized())
                .attributes(parseAttributes(item.getAttributes()))
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
