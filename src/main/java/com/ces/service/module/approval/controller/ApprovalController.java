package com.ces.service.module.approval.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.approval.dto.ApprovalDecisionRequest;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalStatus;
import com.ces.service.module.approval.service.ApprovalService;
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

/** Təsdiqləmələr — the approval queue for deferred destructive actions. */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalRequestResponse>>> list(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<ApprovalRequestResponse> result = approvalService.list(status, pageable);
        PageResponse<ApprovalRequestResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    /** Badge counter for the sidebar — cheap enough to poll. */
    @GetMapping("/pending-count")
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> pendingCount() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", approvalService.countPending())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.get(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> approve(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.approve(id, noteOf(request))));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> reject(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.reject(id, noteOf(request))));
    }

    /** Withdrawing your own pending request — needs no decide permission, only authorship. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.cancel(id)));
    }

    private String noteOf(ApprovalDecisionRequest request) {
        return request == null ? null : request.getNote();
    }

    /**
     * Columns the table is allowed to order by.
     *
     * <p>A whitelist rather than passing the parameter through: the sort string reaches the query
     * as an identifier, so anything the client can name is something the client can ask the
     * database about. Unknown values fall back to the default instead of erroring — a stale
     * bookmark should show the list, not a stack trace.
     */
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
