package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.GarageSettingsRequest;
import com.ces.service.module.garage.dto.GarageSettingsResponse;
import com.ces.service.module.garage.service.GarageSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Qaraj Konfiqurasiya — the branch's Motosaat thresholds (one row, not a list). */
@RestController
@RequestMapping("/api/v1/garage/settings")
public class GarageSettingsController {

    private final GarageSettingsService settingsService;

    public GarageSettingsController(GarageSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<GarageSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.get()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageSettingsResponse>> update(@RequestBody GarageSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.update(request)));
    }
}
