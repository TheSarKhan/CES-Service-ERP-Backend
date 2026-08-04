package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.StocktakeCountRequest;
import com.ces.service.module.inventory.dto.StocktakeOpenRequest;
import com.ces.service.module.inventory.dto.StocktakeResponse;
import com.ces.service.module.inventory.enums.StocktakeStatus;
import com.ces.service.module.inventory.service.StocktakeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** İnventarizasiya — blind counting sheets, one folder at a time. */
@RestController
@RequestMapping("/api/v1/inventory/stocktakes")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    public StocktakeController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<StocktakeResponse>>> list(
            @RequestParam(required = false) StocktakeStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StocktakeResponse> result = stocktakeService.list(
                status,
                PageRequest.of(
                        Math.max(page, 1) - 1,
                        Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "openedAt")));
        PageResponse<StocktakeResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WH_ADJUST')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> open(
            @Valid @RequestBody StocktakeOpenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(stocktakeService.open(request)));
    }

    @PostMapping("/{id}/count")
    @PreAuthorize("hasAuthority('WH_ADJUST')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> count(
            @PathVariable UUID id, @Valid @RequestBody StocktakeCountRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.count(id, request)));
    }

    /** Closing parks one approval for every variance at once; 202 when there is something to review. */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('WH_ADJUST')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> close(@PathVariable UUID id) {
        StocktakeResponse result = stocktakeService.close(id);
        HttpStatus status = result.getStatus() == StocktakeStatus.PENDING_APPROVAL
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WH_ADJUST')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.cancel(id)));
    }
}
