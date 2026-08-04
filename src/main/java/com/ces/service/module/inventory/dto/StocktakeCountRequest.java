package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recording what was actually found on the shelf for one product. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StocktakeCountRequest {

    @NotNull
    private UUID itemId;

    /** Zero is a legitimate answer — it means the shelf was empty, not that nobody looked. */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal countedQuantity;

    @Size(max = 1000)
    private String notes;
}
