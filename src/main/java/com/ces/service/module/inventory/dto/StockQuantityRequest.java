package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for a stock quantity operation. Meaning depends on the endpoint: a delta to add
 * (stock-in), a delta to subtract (stock-out), or the corrected absolute value (adjustment).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockQuantityRequest {

    @NotNull
    private BigDecimal quantity;

    /** Optional reason/note, mainly meaningful for adjustments ("sayım fərqi"). */
    private String reason;
}
