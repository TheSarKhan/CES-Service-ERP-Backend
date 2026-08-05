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

    /**
     * Where the product first goes, and how much of it. Both are read on create only — afterwards
     * location and quantity belong to the stock rows and change through stock operations and
     * moves, never through a product edit.
     */
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

    /**
     * Optional. The entity column is nullable and most real products arrive without one — this was
     * {@code @NotBlank}, which made every such product impossible to edit at all: any save failed
     * validation on a field the record had never carried.
     */
    @Size(max = 255)
    private String barcode;

    @NotBlank
    @Size(max = 50)
    private String unit;

    /** Opening quantity at {@code nodeId}; ignored on update. */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal purchasePrice;

    private Boolean isSerialized;

    /** Batch tracking; cannot be combined with {@code isSerialized}. */
    private Boolean isLotTracked;

    @Min(1)
    private Integer expiryWarningDays;

    /** Dynamic field values, keyed by the owning category's field_key. */
    private Map<String, Object> attributes;

    /** Warranty length in months; drives the end date when one isn't given explicitly. */
    @Min(0)
    private Integer warrantyMonths;

    private LocalDate warrantyStartDate;

    /** Ignored for serialized items — their warranty is tracked per unit. */
    private LocalDate warrantyEndDate;

    /** Reorder point for the total across every folder; null disables the warning. */
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal minQuantity;

    /** Critical level; should sit at or below {@code minQuantity}. */
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal criticalQuantity;

    /** Supplier the warranty claim would be addressed to. */
    @Size(max = 255)
    private String supplier;

    @Size(max = 2000)
    private String notes;

    private Boolean isActive;

    /**
     * Which batch the stock already on the shelf belongs to.
     *
     * <p>Read only when an existing product is switched to batch tracking. The balance is real and
     * physically *is* some batch, so it has to be named: without this the product would report a
     * total its batches do not add up to, and every FEFO suggestion would ignore what is actually
     * there.
     */
    @Size(max = 100)
    private String openingLotNumber;

    private LocalDate openingLotExpiryDate;
}
