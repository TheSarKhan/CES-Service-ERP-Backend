package com.ces.service.module.approval.entity;

/**
 * The deferred operation. Only destructive/mutating actions are listed — creating a record needs
 * no approval, and pure ordering (field reorder) is cosmetic, so both bypass this mechanism.
 */
public enum ApprovalOperation {
    UPDATE,
    DELETE,
    MOVE,
    STOCK_IN,
    STOCK_OUT,
    STOCK_ADJUST,
    FIELD_ADD,
    FIELD_UPDATE,
    FIELD_DELETE,
    /** Moving a warranty end date — reviewed because it decides who pays for a repair. */
    WARRANTY_EXTEND,
    /** Historical, kept so already-decided requests still load. See {@code ApprovalEntityType}. */
    STOCKTAKE_APPLY
}
