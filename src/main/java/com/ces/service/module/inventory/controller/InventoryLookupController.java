package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.inventory.dto.InventoryLookupResponse;
import com.ces.service.module.inventory.service.InventoryLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** QR/Barkod scan resolution — SRS "QR / Barkod Sistemi". */
@RestController
@RequestMapping("/api/v1/inventory/lookup")
public class InventoryLookupController {

    private final InventoryLookupService lookupService;

    public InventoryLookupController(InventoryLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryLookupResponse>> resolve(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.ok(lookupService.resolve(code)));
    }
}
