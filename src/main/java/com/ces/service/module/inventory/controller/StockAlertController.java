package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.StockAlertSummaryResponse;
import com.ces.service.module.inventory.service.StockAlertService;
import org.springframework.data.domain.Page;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Minimum / kritik stok xəbərdarlıqları. */
@RestController
@RequestMapping("/api/v1/inventory/stock-alerts")
public class StockAlertController {

    private final StockAlertService alertService;

    public StockAlertController(StockAlertService alertService) {
        this.alertService = alertService;
    }

    /** Real column names — this listing is a native query. Absent means "worst shortfall first". */
    private static final Set<String> SORTABLE =
            Set.of("name", "sku", "barcode", "unit", "min_quantity", "critical_quantity", "supplier");

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<StockAlertSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.summary()));
    }

    /** Ordered by shortfall, so the product furthest below its threshold is first. */
    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> list(
            @RequestParam(defaultValue = "false") boolean criticalOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        // Unsorted: the ordering lives in the query, which sorts by how far below the line it is.
        Page<InventoryItemResponse> result = alertService.list(
                criticalOnly,
                PageRequest.of(
                        Math.max(page, 1) - 1,
                        Math.min(Math.max(size, 1), 100),
                        sort != null && SORTABLE.contains(sort)
                                ? Sort.by(
                                        "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                                        sort)
                                : StockAlertService.SHORTFALL_FIRST));
        PageResponse<InventoryItemResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }
}
