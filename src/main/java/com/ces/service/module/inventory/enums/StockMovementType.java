package com.ces.service.module.inventory.enums;

/**
 * What caused a stock quantity to change.
 *
 * <p>Kept separate from {@code ApprovalOperation} on purpose: an approval describes what somebody
 * *asked for*, a movement describes what actually happened to the shelf. A relocation is one
 * request but two movements.
 */
public enum StockMovementType {

    /** Goods arriving into a location. */
    IN,

    /** Goods leaving a location. */
    OUT,

    /** Count correction — the signed difference between the counted and recorded quantity. */
    ADJUST,

    /**
     * Leaving the source folder of a relocation ("Köçür").
     *
     * <p>The name predates the removal of the two-step transfer module and is kept because it is
     * written into every existing ledger row and into the table's CHECK constraint.
     */
    TRANSFER_OUT,

    /** Arriving at the destination folder of a relocation ("Köçür"). */
    TRANSFER_IN,

    /** A serialized unit registered at a location. */
    UNIT_IN,

    /** A serialized unit removed from a location. */
    UNIT_OUT
}
