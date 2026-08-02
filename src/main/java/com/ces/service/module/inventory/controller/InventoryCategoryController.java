package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.inventory.dto.InventoryCategoryFieldReorderRequest;
import com.ces.service.module.inventory.dto.InventoryCategoryFieldRequest;
import com.ces.service.module.inventory.dto.InventoryCategoryFieldResponse;
import com.ces.service.module.inventory.dto.InventoryCategoryRequest;
import com.ces.service.module.inventory.dto.InventoryCategoryResponse;
import com.ces.service.module.inventory.service.InventoryCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inventory category + dynamic field configuration endpoints — Stok İdarəetməsi. */
@RestController
@RequestMapping("/api/v1/inventory/categories")
public class InventoryCategoryController {

    private final InventoryCategoryService categoryService;

    public InventoryCategoryController(InventoryCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryCategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> create(
            @Valid @RequestBody InventoryCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── dynamic fields ───────────────────────────────────────────────────

    @GetMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryCategoryFieldResponse>>> listFields(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listFields(id)));
    }

    @PostMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryCategoryFieldResponse>> addField(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(categoryService.addField(id, request)));
    }

    @PutMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryCategoryFieldResponse>> updateField(
            @PathVariable UUID id,
            @PathVariable UUID fieldId,
            @Valid @RequestBody InventoryCategoryFieldRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateField(id, fieldId, request)));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> removeField(@PathVariable UUID id, @PathVariable UUID fieldId) {
        categoryService.removeField(id, fieldId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/fields/reorder")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<List<InventoryCategoryFieldResponse>>> reorderFields(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryFieldReorderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.reorderFields(id, request.getFieldIds())));
    }
}
