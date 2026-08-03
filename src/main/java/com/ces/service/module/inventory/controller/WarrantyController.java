package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitResponse;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import com.ces.service.module.inventory.dto.WarrantyExtensionResponse;
import com.ces.service.module.inventory.dto.WarrantySummaryResponse;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.service.InventoryItemService;
import com.ces.service.module.inventory.service.InventoryItemUnitService;
import com.ces.service.module.inventory.service.WarrantyService;
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

/**
 * Zəmanət — expiry overview and extension requests.
 *
 * <p>Extending never applies straight away: like every other consequential warehouse change it is
 * parked in the Təsdiqləmələr queue (202 Accepted) and applied by {@code WarrantyService} once a
 * second person approves it.
 */
@RestController
@RequestMapping("/api/v1/inventory/warranty")
public class WarrantyController {

    private final WarrantyService warrantyService;
    private final InventoryItemService itemService;
    private final InventoryItemUnitService unitService;
    private final ApprovalService approvalService;

    public WarrantyController(
            WarrantyService warrantyService,
            InventoryItemService itemService,
            InventoryItemUnitService unitService,
            ApprovalService approvalService) {
        this.warrantyService = warrantyService;
        this.itemService = itemService;
        this.unitService = unitService;
        this.approvalService = approvalService;
    }

    /** Counts behind the "bitmək üzrə" badge and dashboard card. */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<WarrantySummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.summary()));
    }

    @GetMapping("/items/{id}/extensions")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<WarrantyExtensionResponse>>> itemHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok(warrantyService.history(WarrantyTargetType.INVENTORY_ITEM, id)));
    }

    @GetMapping("/units/{id}/extensions")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<WarrantyExtensionResponse>>> unitHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok(warrantyService.history(WarrantyTargetType.INVENTORY_ITEM_UNIT, id)));
    }

    @PostMapping("/items/{id}/extend")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> extendItem(
            @PathVariable UUID id, @Valid @RequestBody WarrantyExtendRequest request) {
        InventoryItemResponse before = itemService.get(id);
        // Refused here rather than at approval time: a serialized product holds no warranty of its
        // own, so queuing this would park a request that can only ever fail — and block the item
        // meanwhile, since a pending request locks it.
        if (Boolean.TRUE.equals(before.getIsSerialized())) {
            throw new BusinessException(ErrorCode.ITEM_IS_SERIALIZED);
        }
        return accepted(approvalService.submit(
                ApprovalEntityType.INVENTORY_ITEM,
                id,
                before.getName(),
                ApprovalOperation.WARRANTY_EXTEND,
                request,
                before));
    }

    @PostMapping("/units/{id}/extend")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> extendUnit(
            @PathVariable UUID id, @Valid @RequestBody WarrantyExtendRequest request) {
        InventoryItemUnitResponse before = unitService.get(id);
        return accepted(approvalService.submit(
                ApprovalEntityType.INVENTORY_ITEM_UNIT,
                id,
                before.getSerialNumber(),
                ApprovalOperation.WARRANTY_EXTEND,
                request,
                before));
    }

    private ResponseEntity<ApiResponse<ApprovalRequestResponse>> accepted(ApprovalRequestResponse request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(request));
    }
}
