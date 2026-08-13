package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateRequest;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateResponse;
import com.ces.service.module.garage.service.GarageMaintenanceTemplateService;
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

@RestController
@RequestMapping("/api/v1/garage/maintenance-templates")
public class GarageMaintenanceTemplateController {

    private final GarageMaintenanceTemplateService templateService;

    public GarageMaintenanceTemplateController(GarageMaintenanceTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<GarageMaintenanceTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<GarageMaintenanceTemplateResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageMaintenanceTemplateResponse>> create(
            @Valid @RequestBody GarageMaintenanceTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(templateService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageMaintenanceTemplateResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody GarageMaintenanceTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
