package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.VehicleComponentInstallRequest;
import com.ces.service.module.garage.dto.VehicleComponentRemoveRequest;
import com.ces.service.module.garage.dto.VehicleComponentResponse;
import com.ces.service.module.garage.service.VehicleComponentService;
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
@RequestMapping("/api/v1/vehicles/{vehicleId}/components")
public class VehicleComponentController {

    private final VehicleComponentService componentService;

    public VehicleComponentController(VehicleComponentService componentService) {
        this.componentService = componentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<VehicleComponentResponse>>> list(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(componentService.list(vehicleId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehicleComponentResponse>> install(
            @PathVariable UUID vehicleId, @Valid @RequestBody VehicleComponentInstallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(componentService.install(vehicleId, request)));
    }

    @PostMapping("/{componentId}/remove")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehicleComponentResponse>> remove(
            @PathVariable UUID vehicleId,
            @PathVariable UUID componentId,
            @Valid @RequestBody VehicleComponentRemoveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(componentService.remove(vehicleId, componentId, request)));
    }
}
