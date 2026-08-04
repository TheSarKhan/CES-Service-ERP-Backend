package com.ces.service.module.inventory.enums;

/**
 * Where a warranty claim stands with the supplier.
 *
 * <p>The distinction that matters commercially is {@link #ACCEPTED} vs {@link #REJECTED}: accepted
 * means the supplier carries the cost, rejected means we do. {@link #RESOLVED} closes the file once
 * the replacement/repair actually landed.
 */
public enum WarrantyClaimStatus {

    /** Sent to the supplier, no answer yet. */
    SUBMITTED,

    /** Supplier recognised the warranty — cost is theirs. */
    ACCEPTED,

    /** Supplier refused — cost is ours. */
    REJECTED,

    /** Closed: the replacement, repair or refund actually happened. */
    RESOLVED
}
