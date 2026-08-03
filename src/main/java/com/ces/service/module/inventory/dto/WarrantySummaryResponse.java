package com.ces.service.module.inventory.dto;

/**
 * Counts behind the "zəmanəti bitmək üzrə" badge and dashboard card. Items and serialized units
 * are counted separately because they're acted on in different screens, plus a combined total for
 * the single-number badge.
 */
public record WarrantySummaryResponse(
        long expiringSoonItems,
        long expiringSoonUnits,
        long expiredItems,
        long expiredUnits,
        long expiringSoonTotal,
        long expiredTotal) {

    public static WarrantySummaryResponse of(
            long expiringSoonItems, long expiringSoonUnits, long expiredItems, long expiredUnits) {
        return new WarrantySummaryResponse(
                expiringSoonItems,
                expiringSoonUnits,
                expiredItems,
                expiredUnits,
                expiringSoonItems + expiringSoonUnits,
                expiredItems + expiredUnits);
    }
}
