package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.service.ApprovalExecutor;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.enginehours.dto.MeterRolloverRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Replays an approved meter rollover. See {@code VehicleApprovalExecutor} for the same shape. */
@Component
public class MeterReadingApprovalExecutor implements ApprovalExecutor {

    private final MeterReadingService meterReadingService;
    private final ApprovalService approvalService;

    public MeterReadingApprovalExecutor(MeterReadingService meterReadingService, @Lazy ApprovalService approvalService) {
        this.meterReadingService = meterReadingService;
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalEntityType entityType() {
        return ApprovalEntityType.METER_READING;
    }

    @Override
    public void execute(ApprovalRequest request) {
        switch (request.getOperation()) {
            case METER_ROLLOVER -> meterReadingService.applyRollover(
                    request.getEntityId(), approvalService.readPayload(request, MeterRolloverRequest.class));
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
