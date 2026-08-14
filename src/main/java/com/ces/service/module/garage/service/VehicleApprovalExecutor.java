package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.garage.dto.VehicleRequest;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalRequest;
import com.ces.service.module.garageapproval.service.GarageApprovalExecutor;
import com.ces.service.module.garageapproval.service.GarageApprovalService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Replays an approved vehicle change. The controller parked the original request body verbatim,
 * so approval just deserializes it and calls the same service method the direct route would have.
 */
@Component
public class VehicleApprovalExecutor implements GarageApprovalExecutor {

    private final VehicleService vehicleService;
    private final GarageApprovalService approvalService;

    // @Lazy breaks the cycle: GarageApprovalService collects every executor, and this executor
    // needs GarageApprovalService back for payload deserialization.
    public VehicleApprovalExecutor(VehicleService vehicleService, @Lazy GarageApprovalService approvalService) {
        this.vehicleService = vehicleService;
        this.approvalService = approvalService;
    }

    @Override
    public GarageApprovalEntityType entityType() {
        return GarageApprovalEntityType.VEHICLE;
    }

    @Override
    public void execute(GarageApprovalRequest request) {
        switch (request.getOperation()) {
            case UPDATE -> vehicleService.applyUpdate(
                    request.getEntityId(), approvalService.readPayload(request, VehicleRequest.class));
            case DELETE -> vehicleService.applyDelete(request.getEntityId());
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
