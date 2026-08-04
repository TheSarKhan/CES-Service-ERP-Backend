package com.ces.service.module.inventory.service;

import com.ces.service.module.inventory.enums.StockLevel;
import java.math.BigDecimal;

/**
 * Single source of truth for "is this product running out".
 *
 * <p>Named after {@code WarrantyClock} and for the same reason: the badge, the filter, the summary
 * counts and the daily digest all have to agree, or a product can be listed as low in one place and
 * fine in another.
 */
public final class StockClock {

    private StockClock() {
    }

    /**
     * Thresholds are inclusive: sitting exactly on the reorder point is the moment to reorder, not
     * one unit later. A null threshold means nobody tracks that level, so it never fires.
     */
    public static StockLevel levelOf(BigDecimal total, BigDecimal minQuantity, BigDecimal criticalQuantity) {
        BigDecimal onHand = total == null ? BigDecimal.ZERO : total;
        if (criticalQuantity != null && onHand.compareTo(criticalQuantity) <= 0) {
            return StockLevel.CRITICAL;
        }
        if (minQuantity != null && onHand.compareTo(minQuantity) <= 0) {
            return StockLevel.LOW;
        }
        return StockLevel.OK;
    }
}
