package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registers a batch of serialized units for one item in a single call (e.g. 50 batteries bought
 * on the same invoice, sharing the same purchase/warranty window but each with its own serial).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemUnitBatchCreateRequest {

    @NotEmpty
    private List<String> serialNumbers;

    /** Defaults to the item's current node when omitted. */
    private UUID nodeId;

    /** Defaults to today when omitted. */
    private LocalDate purchaseDate;

    /** Defaults to {@code purchaseDate} when omitted. */
    private LocalDate warrantyStartDate;

    /** Null = no warranty tracked for this batch. */
    private LocalDate warrantyEndDate;

    private String notes;
}
