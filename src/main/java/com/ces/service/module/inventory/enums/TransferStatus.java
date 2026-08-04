package com.ces.service.module.inventory.enums;

/** Where a transfer stands between the two shelves. */
public enum TransferStatus {

    /** Left the source, not yet arrived — the stock is on somebody's trolley. */
    IN_TRANSIT,

    /** Arrived and counted in at the destination. */
    RECEIVED,

    /** Called off while in transit; the stock went back to the source. */
    CANCELLED
}
