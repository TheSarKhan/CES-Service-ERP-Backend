package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Create / update payload for a product. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemRequest {

    @NotNull
    private UUID nodeId;

    @NotNull
    private UUID categoryId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String barcode;

    @NotBlank
    @Size(max = 50)
    private String unit;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal purchasePrice;

    private Boolean isSerialized;

    /** Dynamic field values, keyed by the owning category's field_key. */
    private Map<String, Object> attributes;

    /** Warranty length in months; drives the end date when one isn't given explicitly. */
    @Min(0)
    private Integer warrantyMonths;

    private LocalDate warrantyStartDate;

    /** Ignored for serialized items — their warranty is tracked per unit. */
    private LocalDate warrantyEndDate;

    @Size(max = 2000)
    private String notes;

    private Boolean isActive;
}
