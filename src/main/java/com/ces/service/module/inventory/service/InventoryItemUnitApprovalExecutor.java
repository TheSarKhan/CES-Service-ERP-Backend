package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Replays approved changes to a single serialized unit. Only warranty extension goes through
 * review today — unit status changes (failure, disposal) stay immediate, since they record
 * something that already happened rather than granting anything.
 */
@Component
public class InventoryItemUnitApprovalExecutor implements ApprovalExecutor {

    private final WarrantyService warrantyService;
    private final ApprovalService approvalService;

    public InventoryItemUnitApprovalExecutor(
            WarrantyService warrantyService, @Lazy ApprovalService approvalService) {
        this.warrantyService = warrantyService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.INVENTORY_ITEM_UNIT;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case WARRANTY_EXTEND -> warrantyService.extendUnit(
                    request.getEntityId(), approvalService.readPayload(request, WarrantyExtendRequest.class));
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
