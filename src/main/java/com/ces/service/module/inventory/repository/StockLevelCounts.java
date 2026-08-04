package com.ces.service.module.inventory.repository;

/**
 * How many products sit at each warning level.
 *
 * <p>A product counts once: one that is already critical is not also counted as low, so the two
 * numbers add up to "products needing attention" rather than double-reporting the worst ones.
 */
public interface StockLevelCounts {

    Long getLow();

    Long getCritical();
}
