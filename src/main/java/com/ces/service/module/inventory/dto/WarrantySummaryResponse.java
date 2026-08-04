package com.ces.service.module.inventory.dto;

/**
 * Counts behind the "zəmanəti bitmək üzrə" badge and dashboard card. Items and serialized units
 * are counted separately because they're acted on in different screens, plus a combined total for
 * the single-number badge.
 *
 * <p>{@code openClaims} sits alongside them because it's the third thing that needs chasing: a
 * claim sent to a supplier with no answer yet is money still in the air.
 */
public record WarrantySummaryResponse(
        long expiringSoonItems,
        long expiringSoonUnits,
        long expiredItems,
        long expiredUnits,
        long expiringSoonTotal,
        long expiredTotal,
        long openClaims) {

    public static WarrantySummaryResponse of(
            long expiringSoonItems,
            long expiringSoonUnits,
            long expiredItems,
            long expiredUnits,
            long openClaims) {
        return new WarrantySummaryResponse(
                expiringSoonItems,
                expiringSoonUnits,
                expiredItems,
                expiredUnits,
                expiringSoonItems + expiringSoonUnits,
                expiredItems + expiredUnits,
                openClaims);
    }
}
