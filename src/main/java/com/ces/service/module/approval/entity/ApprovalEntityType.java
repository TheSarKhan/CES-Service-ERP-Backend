package com.ces.service.module.approval.entity;

/** Kind of record an {@link ApprovalRequest} targets — decides which executor replays it. */
public enum ApprovalEntityType {
    INVENTORY_ITEM,
    INVENTORY_ITEM_UNIT,
    INVENTORY_NODE,
    INVENTORY_CATEGORY,
    /** A piece of equipment (Qaraj) — see {@code VehicleApprovalExecutor}. */
    VEHICLE,
    /**
     * Historical. The stocktake sheet was removed, but decided requests still carry this type and
     * dropping the constant would make the approval history fail to load.
     */
    INVENTORY_STOCKTAKE
}
