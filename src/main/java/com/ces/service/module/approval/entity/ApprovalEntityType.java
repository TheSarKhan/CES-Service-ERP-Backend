package com.ces.service.module.approval.entity;

/** Kind of record an {@link ApprovalRequest} targets — decides which executor replays it. */
public enum ApprovalEntityType {
    INVENTORY_ITEM,
    INVENTORY_ITEM_UNIT,
    INVENTORY_NODE,
    INVENTORY_CATEGORY
}
