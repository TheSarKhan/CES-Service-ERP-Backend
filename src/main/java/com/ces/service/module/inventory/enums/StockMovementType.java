package com.ces.service.module.inventory.enums;

/**
 * What caused a stock quantity to change.
 *
 * <p>Kept separate from {@code ApprovalOperation} on purpose: an approval describes what somebody
 * *asked for*, a movement describes what actually happened to the shelf. A transfer is one
 * request but two movements, and a stocktake is one session but many.
 */
public enum StockMovementType {

    /** Goods arriving into a location. */
    IN,

    /** Goods leaving a location. */
    OUT,

    /** Count correction — the signed difference between the counted and recorded quantity. */
    ADJUST,

    /** Leaving the source of a transfer. */
    TRANSFER_OUT,

    /** Arriving at the destination of a transfer. */
    TRANSFER_IN,

    /** A serialized unit registered at a location. */
    UNIT_IN,

    /** A serialized unit removed from a location. */
    UNIT_OUT
}
