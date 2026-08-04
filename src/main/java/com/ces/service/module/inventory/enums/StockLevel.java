package com.ces.service.module.inventory.enums;

/**
 * Where a product's total stock sits against its thresholds.
 *
 * <p>Compared against the total across every folder, not per shelf: "should we reorder?" is a
 * question about the product. An empty shelf while forty sit in the next aisle is a moving problem,
 * not a buying one.
 */
public enum StockLevel {

    /** No threshold set, or comfortably above it. */
    OK,

    /** At or below the reorder point. */
    LOW,

    /** At or below the critical threshold — work stops when this runs out. */
    CRITICAL
}
