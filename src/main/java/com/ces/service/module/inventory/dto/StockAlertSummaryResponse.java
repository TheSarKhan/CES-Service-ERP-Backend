package com.ces.service.module.inventory.dto;

/**
 * Counts behind the low-stock band and sidebar badge.
 *
 * <p>A product is counted once, at its worst level — a critical product is not also counted as low,
 * so {@code total} is "how many need attention" rather than an inflated sum.
 */
public record StockAlertSummaryResponse(long low, long critical, long total) {

    public static StockAlertSummaryResponse of(long low, long critical) {
        return new StockAlertSummaryResponse(low, critical, low + critical);
    }
}
