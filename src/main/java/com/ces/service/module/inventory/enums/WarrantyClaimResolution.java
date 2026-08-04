package com.ces.service.module.inventory.enums;

/** How a warranty claim was finally settled. */
public enum WarrantyClaimResolution {

    /** A new unit was supplied in place of the failed one. */
    REPLACED,

    /** The same unit came back repaired. */
    REPAIRED,

    /** Money back instead of goods. */
    REFUNDED,

    /** Closed without compensation (typically after a rejection). */
    NONE
}
