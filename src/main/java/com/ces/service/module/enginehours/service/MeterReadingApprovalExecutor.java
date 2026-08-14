package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.enginehours.dto.MeterRolloverRequest;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalRequest;
import com.ces.service.module.garageapproval.service.GarageApprovalExecutor;
import com.ces.service.module.garageapproval.service.GarageApprovalService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Replays an approved meter rollover. See {@code VehicleApprovalExecutor} for the same shape. */
@Component
public class MeterReadingApprovalExecutor implements GarageApprovalExecutor {

    private final MeterReadingService meterReadingService;
    private final GarageApprovalService approvalService;

    public MeterReadingApprovalExecutor(
            MeterReadingService meterReadingService, @Lazy GarageApprovalService approvalService) {
        this.meterReadingService = meterReadingService;
        this.approvalService = approvalService;
    }

    @Override
    public GarageApprovalEntityType entityType() {
        return GarageApprovalEntityType.METER_READING;
    }

    @Override
    public void execute(GarageApprovalRequest request) {
        switch (request.getOperation()) {
            case METER_ROLLOVER -> meterReadingService.applyRollover(
                    request.getEntityId(), approvalService.readPayload(request, MeterRolloverRequest.class));
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
