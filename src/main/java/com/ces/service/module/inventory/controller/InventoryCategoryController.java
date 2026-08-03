package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.CategoryFieldApprovalPayload;
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

/**
 * Inventory category + dynamic field configuration endpoints — Stok İdarəetməsi.
 *
 * <p>Changing or removing a category (or one of its dynamic fields) reshapes every product
 * already filed under it, so those actions are deferred to the Təsdiqləmələr queue. Creating a
 * category and reordering existing fields are not: the first adds nothing to review, and the
 * second is pure presentation order — routing a drag-and-drop through approval would make the
 * field list unusable.
 */
@RestController
@RequestMapping("/api/v1/inventory/categories")
public class InventoryCategoryController {

    private final InventoryCategoryService categoryService;
    private final ApprovalService approvalService;

    public InventoryCategoryController(
            InventoryCategoryService categoryService, ApprovalService approvalService) {
        this.categoryService = categoryService;
        this.approvalService = approvalService;
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
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryRequest request) {
        return accepted(submit(id, ApprovalOperation.UPDATE, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> delete(@PathVariable UUID id) {
        return accepted(submit(id, ApprovalOperation.DELETE, null));
    }

    // ── dynamic fields ───────────────────────────────────────────────────

    @GetMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryCategoryFieldResponse>>> listFields(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listFields(id)));
    }

    @PostMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> addField(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryFieldRequest request) {
        return accepted(submit(
                id,
                ApprovalOperation.FIELD_ADD,
                CategoryFieldApprovalPayload.builder().field(request).build()));
    }

    @PutMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> updateField(
            @PathVariable UUID id,
            @PathVariable UUID fieldId,
            @Valid @RequestBody InventoryCategoryFieldRequest request) {
        return accepted(submit(
                id,
                ApprovalOperation.FIELD_UPDATE,
                CategoryFieldApprovalPayload.builder().fieldId(fieldId).field(request).build()));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> removeField(
            @PathVariable UUID id, @PathVariable UUID fieldId) {
        return accepted(submit(
                id,
                ApprovalOperation.FIELD_DELETE,
                CategoryFieldApprovalPayload.builder().fieldId(fieldId).build()));
    }

    /** Presentation order only — applied immediately, see the class javadoc. */
    @PatchMapping("/{id}/fields/reorder")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<List<InventoryCategoryFieldResponse>>> reorderFields(
            @PathVariable UUID id, @Valid @RequestBody InventoryCategoryFieldReorderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.reorderFields(id, request.getFieldIds())));
    }

    private ApprovalRequestResponse submit(UUID id, ApprovalOperation operation, Object payload) {
        InventoryCategoryResponse before = categoryService.get(id);
        return approvalService.submit(
                ApprovalEntityType.INVENTORY_CATEGORY, id, before.getName(), operation, payload, before);
    }

    private ResponseEntity<ApiResponse<ApprovalRequestResponse>> accepted(ApprovalRequestResponse request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(request));
    }
}
