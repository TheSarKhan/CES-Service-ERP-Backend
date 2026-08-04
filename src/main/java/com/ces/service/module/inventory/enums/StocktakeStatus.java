package com.ces.service.module.inventory.enums;

/** Where a stocktake stands. */
public enum StocktakeStatus {

    /** Being counted. Nobody can see the system figures while it is here. */
    OPEN,

    /** Counted and closed; the variances are waiting on a second person. */
    PENDING_APPROVAL,

    /** Approved — the corrections are in the ledger. */
    APPLIED,

    /** Abandoned; nothing was changed. */
    CANCELLED
}
