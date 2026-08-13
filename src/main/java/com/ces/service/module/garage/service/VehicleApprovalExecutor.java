package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.garage.dto.VehicleRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Replays an approved vehicle change. The controller parked the original request body verbatim,
 * so approval just deserializes it and calls the same service method the direct route would have.
 */
@Component
public class VehicleApprovalExecutor implements ApprovalExecutor {

    private final VehicleService vehicleService;
    private final ApprovalService approvalService;

    // @Lazy breaks the cycle: ApprovalService collects every executor, and this executor needs
    // ApprovalService back for payload deserialization.
    public VehicleApprovalExecutor(VehicleService vehicleService, @Lazy ApprovalService approvalService) {
        this.vehicleService = vehicleService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.VEHICLE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case UPDATE -> vehicleService.applyUpdate(
                    request.getEntityId(), approvalService.readPayload(request, VehicleRequest.class));
            case DELETE -> vehicleService.applyDelete(request.getEntityId());
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
