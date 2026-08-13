package com.ces.service.module.enginehours.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.enginehours.dto.ApplyTemplateRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenancePlanRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenancePlanResponse;
import com.ces.service.module.enginehours.service.VehicleMaintenancePlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A vehicle's own periodic-maintenance plan — texnikaya kliklənərək əlçatan (brifin UX qərarı). */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/maintenance-plans")
public class VehicleMaintenancePlanController {

    private final VehicleMaintenancePlanService planService;

    public VehicleMaintenancePlanController(VehicleMaintenancePlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EH_READ')")
    public ResponseEntity<ApiResponse<List<VehicleMaintenancePlanResponse>>> list(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(planService.list(vehicleId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<VehicleMaintenancePlanResponse>> create(
            @PathVariable UUID vehicleId, @Valid @RequestBody VehicleMaintenancePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(planService.create(vehicleId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<VehicleMaintenancePlanResponse>> update(
            @PathVariable UUID vehicleId, @PathVariable UUID id, @Valid @RequestBody VehicleMaintenancePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planService.update(vehicleId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID vehicleId, @PathVariable UUID id) {
        planService.delete(vehicleId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/apply-template")
    @PreAuthorize("hasAuthority('EH_CREATE')")
    public ResponseEntity<ApiResponse<List<VehicleMaintenancePlanResponse>>> applyTemplate(
            @PathVariable UUID vehicleId, @Valid @RequestBody ApplyTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(planService.applyTemplate(vehicleId, request.getTemplateId())));
    }
}
