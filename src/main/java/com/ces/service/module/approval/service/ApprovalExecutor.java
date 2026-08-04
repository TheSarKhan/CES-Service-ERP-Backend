package com.ces.service.module.approval.service;

import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;

/**
 * Replays an approved request against its owning module.
 *
 * <p>Keeps the approval module free of any inventory (or future module) knowledge: each module
 * contributes its own executor, and {@link ApprovalService} just dispatches by entity type.
 */
public interface ApprovalExecutor {

    ApprovalEntityType entityType();

    /**
     * Applies the deferred operation. Runs inside the approving transaction, so throwing here
     * aborts the approval and leaves the request PENDING — the decision is never recorded for a
     * change that failed to apply.
     */
    void execute(ApprovalRequest request);

    /**
     * Lets a module react to its request being turned down or withdrawn.
     *
     * <p>Most records need nothing: rejecting an edit simply means it never happened. A record that
     * parked itself in a waiting state to make the request — a closed stocktake, say — has to be
     * let out of it, or it stays stuck and blocks the next one forever.
     */
    default void onNotApplied(ApprovalRequest request) {
        // Nothing to undo by default.
    }
}
