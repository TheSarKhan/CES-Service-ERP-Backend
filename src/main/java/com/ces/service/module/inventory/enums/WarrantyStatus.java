package com.ces.service.module.inventory.enums;

/** Computed (not persisted) warranty state of an {@code InventoryItemUnit}, derived from today's date. */
public enum WarrantyStatus {
    /** No warranty end date set for this unit. */
    NONE,
    ACTIVE,
    /** Ends within the next 30 days. */
    EXPIRING_SOON,
    EXPIRED
}
