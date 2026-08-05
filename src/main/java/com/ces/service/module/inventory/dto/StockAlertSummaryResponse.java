package com.ces.service.module.inventory.dto;

/**
 * Counts behind the warehouse attention band and the sidebar badge.
 *
 * <p>A product is counted once, at its worst level — a critical product is not also counted as low,
 * so {@code total} is "how many need attention" rather than an inflated sum.
 *
 * <p>Batch counts ride along because they are the same kind of signal — something needs looking at
 * today — and they appear in the same band. They are deliberately left out of {@code total}: that
 * number drives the sidebar badge, which counts products, and quietly folding batches into it would
 * change what a number already on screen means.
 */
public record StockAlertSummaryResponse(
        long low, long critical, long total, long expiringLots, long expiredLots) {

    public static StockAlertSummaryResponse of(
            long low, long critical, long expiringLots, long expiredLots) {
        return new StockAlertSummaryResponse(low, critical, low + critical, expiringLots, expiredLots);
    }
}
