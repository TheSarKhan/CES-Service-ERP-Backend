package com.ces.service.module.approval.entity;

/** Lifecycle of an approval request. Only {@code PENDING} locks its target entity. */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
