package com.ces.service.module.garageapproval.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.approval.dto.ApprovalDecisionRequest;
import com.ces.service.module.approval.entity.ApprovalStatus;
import com.ces.service.module.garageapproval.dto.GarageApprovalRequestResponse;
import com.ces.service.module.garageapproval.service.GarageApprovalService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Qaraj Təsdiqləmələr — the approval queue for deferred Qaraj/Motosaat actions, kept off Anbarın own. */
@RestController
@RequestMapping("/api/v1/garage/approvals")
public class GarageApprovalController {

    private final GarageApprovalService approvalService;

    public GarageApprovalController(GarageApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_READ')")
    public ResponseEntity<ApiResponse<PageResponse<GarageApprovalRequestResponse>>> list(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<GarageApprovalRequestResponse> result = approvalService.list(status, pageable);
        PageResponse<GarageApprovalRequestResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    /** Badge counter for the sidebar — cheap enough to poll. */
    @GetMapping("/pending-count")
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_READ')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> pendingCount() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", approvalService.countPending())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_READ')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.get(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> approve(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.approve(id, noteOf(request))));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> reject(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.reject(id, noteOf(request))));
    }

    /** Withdrawing your own pending request — needs no decide permission, only authorship. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('GARAGE_APPROVAL_READ')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.cancel(id)));
    }

    private String noteOf(ApprovalDecisionRequest request) {
        return request == null ? null : request.getNote();
    }

    private static final Set<String> SORTABLE =
            Set.of("requestedAt", "decidedAt", "status", "operation", "entityType", "entityLabel");

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        String field = SORTABLE.contains(sort) ? sort : "requestedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, field));
    }
}
