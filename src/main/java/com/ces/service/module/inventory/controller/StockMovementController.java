package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.StockMovementResponse;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.service.StockMovementService;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Stok hərəkət tarixçəsi — read-only by design; the ledger is written by StockLedger alone. */
@RestController
@RequestMapping("/api/v1/inventory/stock-movements")
public class StockMovementController {

    private final StockMovementService movementService;

    public StockMovementController(StockMovementService movementService) {
        this.movementService = movementService;
    }

    /** Columns the ledger may be reordered by; anything else falls back to the default. */
    private static final Set<String> SORTABLE =
            Set.of("createdAt", "movementType", "quantity", "balanceAfter");

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>> search(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) UUID nodeId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        // Newest first by default: a ledger is read from the top down.
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), 100),
                Sort.by(
                        "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                        SORTABLE.contains(sort) ? sort : "createdAt"));
        Page<StockMovementResponse> result = movementService.search(itemId, nodeId, type, pageable);
        PageResponse<StockMovementResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }
}
