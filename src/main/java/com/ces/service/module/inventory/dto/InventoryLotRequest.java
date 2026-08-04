package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registering a batch that has arrived at a folder. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLotRequest {

    @NotNull
    private UUID nodeId;

    @NotBlank
    @Size(max = 100)
    private String lotNumber;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal quantity;

    /** Null for batches that do not expire; they sort last under FEFO. */
    private LocalDate expiryDate;

    private LocalDate receivedDate;

    @Size(max = 1000)
    private String notes;
}
