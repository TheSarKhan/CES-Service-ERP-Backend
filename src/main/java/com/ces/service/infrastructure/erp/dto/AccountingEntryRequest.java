package com.ces.service.infrastructure.erp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Accounting push payload sent to CES ERP when a work order is closed
 * (SRS §M13.3).
 *
 * @param entryType   e.g. {@code SERVICE_INCOME}
 * @param woNumber    human-readable work-order number
 * @param branchId    branch UUID
 * @param customerId  ERP-side customer UUID
 * @param closedAt    close timestamp
 * @param woType      work-order type (e.g. PAID_SERVICE)
 * @param items       line items
 * @param totalCost   total cost
 * @param totalSell   total sell amount
 * @param totalProfit total profit
 */
public record AccountingEntryRequest(
        String entryType,
        String woNumber,
        UUID branchId,
        UUID customerId,
        Instant closedAt,
        String woType,
        List<Item> items,
        BigDecimal totalCost,
        BigDecimal totalSell,
        BigDecimal totalProfit
) {

    /**
     * A single accounting line item.
     *
     * @param type        LABOR | PART
     * @param description line description
     * @param totalCost   cost
     * @param totalSell   sell amount
     */
    public record Item(
            String type,
            String description,
            BigDecimal totalCost,
            BigDecimal totalSell
    ) {
    }
}
