package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryItemRequest;
import com.ces.service.module.inventory.dto.MoveItemRequest;
import com.ces.service.module.inventory.dto.StockQuantityRequest;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Replays an approved product change. The controller parked the original request body verbatim,
 * so approval just deserializes it and calls the same service method the direct route would have.
 */
@Component
public class InventoryItemApprovalExecutor implements ApprovalExecutor {

    private final InventoryItemService itemService;
    private final WarrantyService warrantyService;
    private final ApprovalService approvalService;

    // @Lazy breaks the cycle: ApprovalService collects every executor, and this executor needs
    // ApprovalService back for payload deserialization.
    public InventoryItemApprovalExecutor(
            InventoryItemService itemService,
            WarrantyService warrantyService,
            @Lazy ApprovalService approvalService) {
        this.itemService = itemService;
        this.warrantyService = warrantyService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.INVENTORY_ITEM;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case UPDATE -> itemService.update(
                    request.getEntityId(), approvalService.readPayload(request, InventoryItemRequest.class));
            case DELETE -> itemService.delete(request.getEntityId());
            case MOVE -> {
                MoveItemRequest move = approvalService.readPayload(request, MoveItemRequest.class);
                itemService.move(request.getEntityId(), move.getFromNodeId(), move.getToNodeId());
            }
            case STOCK_IN -> {
                StockQuantityRequest stock = approvalService.readPayload(request, StockQuantityRequest.class);
                itemService.increaseQuantity(
                        request.getEntityId(), stock.getNodeId(), stock.getQuantity(), stock.getReason());
            }
            case STOCK_OUT -> {
                StockQuantityRequest stock = approvalService.readPayload(request, StockQuantityRequest.class);
                itemService.decreaseQuantity(
                        request.getEntityId(), stock.getNodeId(), stock.getQuantity(), stock.getReason());
            }
            case STOCK_ADJUST -> {
                StockQuantityRequest stock = approvalService.readPayload(request, StockQuantityRequest.class);
                itemService.adjustQuantity(
                        request.getEntityId(), stock.getNodeId(), stock.getQuantity(), stock.getReason());
            }
            case WARRANTY_EXTEND -> warrantyService.extendItem(
                    request.getEntityId(), approvalService.readPayload(request, WarrantyExtendRequest.class));
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
