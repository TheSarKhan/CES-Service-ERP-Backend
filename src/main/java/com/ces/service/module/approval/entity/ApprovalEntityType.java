package com.ces.service.module.approval.entity;

/**
 * Kind of record an {@link ApprovalRequest} targets — decides which executor replays it.
 *
 * <p>Qaraj/Motosaat (VEHICLE, METER_READING) moved out to their own
 * {@code module.garageapproval} table (migration V49) — this queue is Anbar-only now.
 */
public enum ApprovalEntityType {
    INVENTORY_ITEM,
    INVENTORY_ITEM_UNIT,
    INVENTORY_NODE,
    INVENTORY_CATEGORY,
    /**
     * Historical. The stocktake sheet was removed, but decided requests still carry this type and
     * dropping the constant would make the approval history fail to load.
     */
    INVENTORY_STOCKTAKE
}
