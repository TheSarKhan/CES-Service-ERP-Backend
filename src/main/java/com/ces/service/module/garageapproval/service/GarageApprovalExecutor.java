package com.ces.service.module.garageapproval.service;

import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalRequest;

/**
 * Replays an approved request against its owning module. Same shape as Anbarın
 * {@code ApprovalExecutor} — kept a distinct interface because it dispatches over
 * {@link GarageApprovalEntityType}, not Anbarın {@code ApprovalEntityType}.
 */
public interface GarageApprovalExecutor {

    GarageApprovalEntityType entityType();

    /**
     * Applies the deferred operation. Runs inside the approving transaction, so throwing here
     * aborts the approval and leaves the request PENDING.
     */
    void execute(GarageApprovalRequest request);
}
