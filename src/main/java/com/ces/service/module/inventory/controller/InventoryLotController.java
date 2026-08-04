package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.InventoryLotRequest;
import com.ces.service.module.inventory.dto.InventoryLotResponse;
import com.ces.service.module.inventory.service.InventoryLotService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

/** Partiya (lot) və son istifadə tarixi. */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryLotController {

    private final InventoryLotService lotService;

    public InventoryLotController(InventoryLotService lotService) {
        this.lotService = lotService;
    }

    @GetMapping("/items/{itemId}/lots")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<InventoryLotResponse>>> listForItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(lotService.listForItem(itemId)));
    }

    /** The batch FEFO would pick at a folder. Null body when there is nothing to pick. */
    @GetMapping("/items/{itemId}/lots/suggestion")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryLotResponse>> suggest(
            @PathVariable UUID itemId, @RequestParam UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.ok(lotService.suggestFor(itemId, nodeId)));
    }

    @PostMapping("/items/{itemId}/lots")
    @PreAuthorize("hasAuthority('WH_STOCK_IN')")
    public ResponseEntity<ApiResponse<InventoryLotResponse>> receive(
            @PathVariable UUID itemId, @Valid @RequestBody InventoryLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(lotService.receive(itemId, request)));
    }

    @PostMapping("/lots/{lotId}/consume")
    @PreAuthorize("hasAuthority('WH_USE')")
    public ResponseEntity<ApiResponse<InventoryLotResponse>> consume(
            @PathVariable UUID lotId, @RequestBody Map<String, Object> body) {
        BigDecimal quantity = new BigDecimal(String.valueOf(body.get("quantity")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return ResponseEntity.ok(ApiResponse.ok(lotService.consume(lotId, quantity, reason)));
    }

    @DeleteMapping("/lots/{lotId}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<Void> writeOff(
            @PathVariable UUID lotId, @RequestParam(required = false) String reason) {
        lotService.writeOff(lotId, reason);
        return ResponseEntity.noContent().build();
    }

    /** Batches running out of time, soonest first. */
    @GetMapping("/lots/expiring")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryLotResponse>>> expiring(
            @RequestParam(defaultValue = "30") int withinDays,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryLotResponse> result = lotService.expiring(
                withinDays, PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)));
        PageResponse<InventoryLotResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }
}
