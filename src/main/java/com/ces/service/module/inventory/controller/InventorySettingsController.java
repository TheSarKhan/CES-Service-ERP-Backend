package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.inventory.dto.InventorySettingsRequest;
import com.ces.service.module.inventory.dto.InventorySettingsResponse;
import com.ces.service.module.inventory.service.InventorySettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Anbar tənzimləmələri — branch-scoped policy, not a stock change, so no approval queue. */
@RestController
@RequestMapping("/api/v1/inventory/settings")
public class InventorySettingsController {

    private final InventorySettingsService settingsService;

    public InventorySettingsController(InventorySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventorySettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.get()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventorySettingsResponse>> update(
            @Valid @RequestBody InventorySettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.update(request)));
    }
}
