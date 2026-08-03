package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
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

/** Inventory Layer (dynamic storage tree) endpoints — Stok İdarəetməsi. */
@RestController
@RequestMapping("/api/v1/inventory/nodes")
public class InventoryNodeController {

    private final InventoryNodeService nodeService;

    public InventoryNodeController(InventoryNodeService nodeService) {
        this.nodeService = nodeService;
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
    public ResponseEntity<ApiResponse<InventoryNodeResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryNodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(nodeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        nodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
