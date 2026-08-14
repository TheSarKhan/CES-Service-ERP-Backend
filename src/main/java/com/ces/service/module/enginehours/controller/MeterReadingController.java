package com.ces.service.module.enginehours.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.enginehours.dto.MeterReadingRequest;
import com.ces.service.module.enginehours.dto.MeterReadingResponse;
import com.ces.service.module.enginehours.dto.MeterRolloverRequest;
import com.ces.service.module.enginehours.service.MeterReadingService;
import com.ces.service.module.garage.service.VehicleService;
import com.ces.service.module.garageapproval.dto.GarageApprovalRequestResponse;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalOperation;
import com.ces.service.module.garageapproval.service.GarageApprovalService;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Motosaat/KM history endpoints, nested under a vehicle — texnikaya kliklənərək əlçatan (brifin
 * UX qərarı).
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/meter-readings")
public class MeterReadingController {

    /** Columns the history table may be ordered by; anything else falls back to the default. */
    private static final Set<String> SORTABLE = Set.of("recordedAt", "value", "meterType", "source", "createdAt");

    private final MeterReadingService meterReadingService;
    private final VehicleService vehicleService;
    private final GarageApprovalService approvalService;

    public MeterReadingController(
            MeterReadingService meterReadingService, VehicleService vehicleService, GarageApprovalService approvalService) {
        this.meterReadingService = meterReadingService;
        this.vehicleService = vehicleService;
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingResponse>>> history(
            @PathVariable UUID vehicleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Page<MeterReadingResponse> result = meterReadingService.history(vehicleId, toPageable(page, size, sort, dir));
        PageResponse<MeterReadingResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
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
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> rollover(
            @PathVariable UUID vehicleId, @Valid @RequestBody MeterRolloverRequest request) {
        meterReadingService.assertRolloverValid(vehicleId, request);
        String vehicleName = vehicleService.get(vehicleId).getName();
        GarageApprovalRequestResponse submitted = approvalService.submit(
                GarageApprovalEntityType.METER_READING, vehicleId, vehicleName,
                GarageApprovalOperation.METER_ROLLOVER, request, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(submitted));
    }

    /** `recordedAt` is date-only, so several readings can share a day — createdAt breaks ties. */
    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        String field = SORTABLE.contains(sort) ? sort : "recordedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort order = Sort.by(direction, field);
        if (!field.equals("createdAt")) {
            order = order.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return PageRequest.of(pageIndex, pageSize, order);
    }
}
