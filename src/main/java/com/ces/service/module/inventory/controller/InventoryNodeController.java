package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryNodeRequest;
import com.ces.service.module.inventory.dto.InventoryNodeResponse;
import com.ces.service.module.inventory.service.InventoryNodeService;
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

/**
 * Inventory Layer (dynamic storage tree) endpoints — Stok İdarəetməsi.
 *
 * <p>Editing and deleting a folder are deferred to the Təsdiqləmələr queue (202 Accepted);
 * creating one is not, since it changes nothing that already exists.
 */
@RestController
@RequestMapping("/api/v1/inventory/nodes")
public class InventoryNodeController {

    private final InventoryNodeService nodeService;
    private final ApprovalService approvalService;

    public InventoryNodeController(InventoryNodeService nodeService, ApprovalService approvalService) {
        this.nodeService = nodeService;
        this.approvalService = approvalService;
    }

    /** Children of {@code parentId}, or root nodes when omitted. */
    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryNodeResponse>>> listChildren(
            @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(ApiResponse.ok(nodeService.listChildren(parentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryNodeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(nodeService.get(id)));
    }

    /** Ancestor chain from the root down to (and including) this node — powers "jump to location". */
    @GetMapping("/{id}/path")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryNodeResponse>>> getPath(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(nodeService.getPath(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryNodeResponse>> create(
            @Valid @RequestBody InventoryNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(nodeService.create(request)));
    }

    /** Also used to move a node (parentId change), with cycle prevention. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryNodeRequest request) {
        return accepted(submit(id, ApprovalOperation.UPDATE, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> delete(@PathVariable UUID id) {
        return accepted(submit(id, ApprovalOperation.DELETE, null));
    }

    private ApprovalRequestResponse submit(UUID id, ApprovalOperation operation, Object payload) {
        InventoryNodeResponse before = nodeService.get(id);
        return approvalService.submit(
                ApprovalEntityType.INVENTORY_NODE, id, before.getName(), operation, payload, before);
    }

    private ResponseEntity<ApiResponse<ApprovalRequestResponse>> accepted(ApprovalRequestResponse request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(request));
    }
}
