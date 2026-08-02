package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitBatchCreateRequest;
import com.ces.service.module.inventory.dto.InventoryItemUnitResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitUpdateRequest;
import com.ces.service.module.inventory.dto.MarkUnitFailedRequest;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.service.InventoryItemUnitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

/** Serialized unit + warranty tracking endpoints — Stok İdarəetməsi. */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryItemUnitController {

    private final InventoryItemUnitService unitService;

    public InventoryItemUnitController(InventoryItemUnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping("/items/{itemId}/units")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryItemUnitResponse>>> listByItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.listByItem(itemId)));
    }

    @PostMapping("/items/{itemId}/units")
    @PreAuthorize("hasAuthority('WH_STOCK_IN')")
    public ResponseEntity<ApiResponse<List<InventoryItemUnitResponse>>> createBatch(
            @PathVariable UUID itemId, @Valid @RequestBody InventoryItemUnitBatchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(unitService.createBatch(itemId, request)));
    }

    /** Warranty / general search across all serialized units in the branch. */
    @GetMapping("/item-units")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemUnitResponse>>> search(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) InventoryUnitStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<InventoryItemUnitResponse> result = unitService.search(itemId, status, search, pageable);
        PageResponse<InventoryItemUnitResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/item-units/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryItemUnitResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.get(id)));
    }

    @PutMapping("/item-units/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryItemUnitResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryItemUnitUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.update(id, request)));
    }

    @PostMapping("/item-units/{id}/fail")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryItemUnitResponse>> markFailed(
            @PathVariable UUID id, @Valid @RequestBody MarkUnitFailedRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(unitService.markFailed(id, request)));
    }

    @DeleteMapping("/item-units/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sort));
    }
}
