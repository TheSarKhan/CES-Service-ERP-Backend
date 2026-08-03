package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryNodeRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Replays an approved folder (Layer node) change — see {@link InventoryItemApprovalExecutor}. */
@Component
public class InventoryNodeApprovalExecutor implements ApprovalExecutor {

    private final InventoryNodeService nodeService;
    private final ApprovalService approvalService;

    public InventoryNodeApprovalExecutor(
            InventoryNodeService nodeService, @Lazy ApprovalService approvalService) {
        this.nodeService = nodeService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.INVENTORY_NODE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case UPDATE -> nodeService.update(
                    request.getEntityId(), approvalService.readPayload(request, InventoryNodeRequest.class));
            case DELETE -> nodeService.delete(request.getEntityId());
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
