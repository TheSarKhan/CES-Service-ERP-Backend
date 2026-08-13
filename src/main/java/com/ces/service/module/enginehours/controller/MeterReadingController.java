package com.ces.service.module.enginehours.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.enginehours.dto.MeterReadingRequest;
import com.ces.service.module.enginehours.dto.MeterReadingResponse;
import com.ces.service.module.enginehours.dto.MeterRolloverRequest;
import com.ces.service.module.enginehours.service.MeterReadingService;
import com.ces.service.module.garage.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Motosaat/KM history endpoints, nested under a vehicle — texnikaya kliklənərək əlçatan (brifin
 * UX qərarı).
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/meter-readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;
    private final VehicleService vehicleService;
    private final ApprovalService approvalService;

    public MeterReadingController(
            MeterReadingService meterReadingService, VehicleService vehicleService, ApprovalService approvalService) {
        this.meterReadingService = meterReadingService;
        this.vehicleService = vehicleService;
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EH_READ')")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> history(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(meterReadingService.history(vehicleId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> record(
            @PathVariable UUID vehicleId, @Valid @RequestBody MeterReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(meterReadingService.record(vehicleId, request)));
    }

    @PostMapping("/rollover")
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> rollover(
            @PathVariable UUID vehicleId, @Valid @RequestBody MeterRolloverRequest request) {
        meterReadingService.assertRolloverValid(vehicleId, request);
        String vehicleName = vehicleService.get(vehicleId).getName();
        ApprovalRequestResponse submitted = approvalService.submit(
                ApprovalEntityType.METER_READING, vehicleId, vehicleName,
                ApprovalOperation.METER_ROLLOVER, request, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(submitted));
    }
}
