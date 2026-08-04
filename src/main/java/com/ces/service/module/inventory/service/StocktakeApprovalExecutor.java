package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import org.springframework.stereotype.Component;

/**
 * Applies an approved counting sheet.
 *
 * <p>Unlike the other executors this replays nothing from the payload: the counted figures are
 * already on the sheet's own lines, and re-sending two hundred of them through an approval body
 * would only create a second copy that could disagree with the first.
 */
@Component
public class StocktakeApprovalExecutor implements ApprovalExecutor {

    private final StocktakeService stocktakeService;

    public StocktakeApprovalExecutor(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.INVENTORY_STOCKTAKE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getOperation() != com.ces.service.module.approval.entity.ApprovalOperation.STOCKTAKE_APPLY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        stocktakeService.apply(request.getEntityId());
    }

    /**
     * A rejected or withdrawn sheet has to come out of PENDING_APPROVAL. Left there it would keep
     * the folder locked against a new count forever, since only one sheet per folder may be active.
     */
    @Override
    public void onNotApplied(ApprovalRequest request) {
        stocktakeService.markRejected(request.getEntityId());
    }
}
