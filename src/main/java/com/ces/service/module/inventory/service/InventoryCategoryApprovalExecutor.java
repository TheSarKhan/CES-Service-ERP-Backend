package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.CategoryFieldApprovalPayload;
import com.ces.service.module.inventory.dto.InventoryCategoryRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Replays an approved category / dynamic-field change — see {@link InventoryItemApprovalExecutor}. */
@Component
public class InventoryCategoryApprovalExecutor implements ApprovalExecutor {

    private final InventoryCategoryService categoryService;
    private final ApprovalService approvalService;

    public InventoryCategoryApprovalExecutor(
            InventoryCategoryService categoryService, @Lazy ApprovalService approvalService) {
        this.categoryService = categoryService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.INVENTORY_CATEGORY;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case UPDATE -> categoryService.update(
                    request.getEntityId(), approvalService.readPayload(request, InventoryCategoryRequest.class));
            case DELETE -> categoryService.delete(request.getEntityId());
            case FIELD_ADD -> categoryService.addField(
                    request.getEntityId(), fieldPayload(request).getField());
            case FIELD_UPDATE -> {
                CategoryFieldApprovalPayload payload = fieldPayload(request);
                categoryService.updateField(request.getEntityId(), payload.getFieldId(), payload.getField());
            }
            case FIELD_DELETE -> categoryService.removeField(
                    request.getEntityId(), fieldPayload(request).getFieldId());
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private CategoryFieldApprovalPayload fieldPayload(ApprovalRequest request) {
        return approvalService.readPayload(request, CategoryFieldApprovalPayload.class);
    }
}
