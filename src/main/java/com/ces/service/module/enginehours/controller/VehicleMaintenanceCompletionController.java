package com.ces.service.module.enginehours.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionResponse;
import com.ces.service.module.enginehours.service.VehicleMaintenanceCompletionService;
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

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}")
public class VehicleMaintenanceCompletionController {

    private final VehicleMaintenanceCompletionService completionService;

    public VehicleMaintenanceCompletionController(VehicleMaintenanceCompletionService completionService) {
        this.completionService = completionService;
    }

    /** Full completion history for the vehicle, newest first — every plan's line together. */
    @GetMapping("/maintenance-completions")
    @PreAuthorize("hasAuthority('EH_READ')")
    public ResponseEntity<ApiResponse<List<VehicleMaintenanceCompletionResponse>>> list(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(completionService.list(vehicleId)));
    }

    @PostMapping("/maintenance-plans/{planId}/complete")
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<VehicleMaintenanceCompletionResponse>> complete(
            @PathVariable UUID vehicleId,
            @PathVariable UUID planId,
            @Valid @RequestBody VehicleMaintenanceCompletionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(completionService.complete(vehicleId, planId, request)));
    }
}
