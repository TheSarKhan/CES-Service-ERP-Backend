package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /inventory/items/{id}/move payload — relocates stock from one folder to another.
 *
 * <p>The source is explicit: a product may sit in several folders, so "move it" has to say which
 * stock is being moved.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveItemRequest {

    @NotNull
    private UUID fromNodeId;

    @NotNull
    private UUID toNodeId;

    /**
     * How much to move. Null means the whole balance — the folder stops holding this product at
     * all, which is what the action did before partial moves existed and still the common case.
     *
     * <p>A smaller amount splits the balance instead, leaving the product filed in both folders.
     * Only meaningful for non-serialized products: a serialized one's balance is derived from its
     * units, so there the units are what move.
     */
    @Positive
    private BigDecimal quantity;
}
