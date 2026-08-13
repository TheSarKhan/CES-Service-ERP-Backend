package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.GarageConfigValueRequest;
import com.ces.service.module.garage.dto.GarageConfigValueResponse;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.service.GarageConfigService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Qaraj Konfiqurasiya — every manageable dropdown list in one endpoint set. */
@RestController
@RequestMapping("/api/v1/garage/config/values")
public class GarageConfigController {

    private final GarageConfigService configService;

    public GarageConfigController(GarageConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<GarageConfigValueResponse>>> list(
            @RequestParam(required = false) GarageConfigListType listType) {
        return ResponseEntity.ok(ApiResponse.ok(configService.list(listType)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageConfigValueResponse>> create(
            @Valid @RequestBody GarageConfigValueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(configService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageConfigValueResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody GarageConfigValueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(configService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        configService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
