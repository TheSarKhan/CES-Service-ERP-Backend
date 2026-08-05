package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitResponse;
import com.ces.service.module.inventory.dto.WarrantyClaimDecisionRequest;
import com.ces.service.module.inventory.dto.WarrantyClaimRequest;
import com.ces.service.module.inventory.dto.WarrantyClaimResponse;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import com.ces.service.module.inventory.dto.WarrantyExtensionResponse;
import com.ces.service.module.inventory.dto.WarrantyRecordResponse;
import com.ces.service.module.inventory.dto.WarrantyRecordSearchCriteria;
import com.ces.service.module.inventory.dto.WarrantySummaryResponse;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import com.ces.service.module.inventory.enums.WarrantyRecordType;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import com.ces.service.module.inventory.service.InventoryItemService;
import com.ces.service.module.inventory.service.InventoryItemUnitService;
import com.ces.service.module.inventory.service.WarrantyService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * The warranty search itself — serialized units and non-serialized products together.
     *
     * <p>Results are always ordered soonest-expiry-first, so there is no {@code sort} parameter:
     * the screen's job is "what lapses next", and a caller-chosen sort would only hide that.
     */
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<WarrantyRecordResponse>>> records(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WarrantyRecordType recordType,
            @RequestParam(required = false) WarrantyStatus warrantyStatus,
            @RequestParam(required = false) InventoryUnitStatus unitStatus,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTo,
            @RequestParam(required = false) Integer withinDays,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        WarrantyRecordSearchCriteria criteria = WarrantyRecordSearchCriteria.builder()
                .search(search)
                .recordType(recordType)
                .warrantyStatus(warrantyStatus)
                .unitStatus(unitStatus)
                .supplier(supplier)
                .endFrom(endFrom)
                .endTo(endTo)
                .withinDays(withinDays)
                .build();
        // Unsorted on purpose — the ordering lives in the query; see WarrantyRecordRepository.
        Page<WarrantyRecordResponse> result = warrantyService.search(criteria, toPageable(page, size));
        PageResponse<WarrantyRecordResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    /** Suppliers in use, for the filter dropdown. */
    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<String>>> suppliers() {
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.suppliers()));
    }

    // ── Claims ───────────────────────────────────────────────────────────

    /**
     * Columns the claims table may order by. A whitelist, not a pass-through: the value reaches the
     * query as an identifier, and an unknown one falls back to the default rather than erroring so
     * a stale bookmark still shows the list.
     */
    private static final Set<String> CLAIM_SORTABLE =
            Set.of("createdAt", "submittedAt", "decidedAt", "status", "supplier", "claimNumber");

    @GetMapping("/claims")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<WarrantyClaimResponse>>> claims(
            @RequestParam(required = false) WarrantyClaimStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), 100),
                Sort.by(
                        "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                        CLAIM_SORTABLE.contains(sort) ? sort : "createdAt"));
        PageResponse<WarrantyClaimResponse> body =
                PageResponse.of(warrantyService.listClaims(status, search, pageable));
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/claims/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> claim(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.getClaim(id)));
    }

    /** Every claim ever filed for one product or unit, newest first. */
    @GetMapping("/targets/{targetType}/{targetId}/claims")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> targetClaims(
            @PathVariable WarrantyTargetType targetType, @PathVariable UUID targetId) {
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.claimsForTarget(targetType, targetId)));
    }

    /**
     * Files a claim. Unlike extending a warranty this applies immediately: it records something
     * that already happened outside the system rather than changing stock or a warranty window, so
     * parking it behind an approval would only put the record behind the fact.
     */
    @PostMapping("/claims")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> createClaim(
            @Valid @RequestBody WarrantyClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(warrantyService.createClaim(request)));
    }

    /** Records the supplier's answer — accepted (they pay) or rejected (we do). */
    @PostMapping("/claims/{id}/decision")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> decideClaim(
            @PathVariable UUID id, @Valid @RequestBody WarrantyClaimDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.decideClaim(id, request)));
    }

    @DeleteMapping("/claims/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> deleteClaim(@PathVariable UUID id) {
        warrantyService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }

    // ── Extensions ───────────────────────────────────────────────────────

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

    /** Pages are 1-based on the wire (SRS §6.3) and unsorted unless the caller adds a sort. */
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
    }
}
