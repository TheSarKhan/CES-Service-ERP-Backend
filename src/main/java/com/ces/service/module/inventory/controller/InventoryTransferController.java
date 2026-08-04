package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.inventory.dto.TransferRequest;
import com.ces.service.module.inventory.dto.TransferResponse;
import com.ces.service.module.inventory.enums.TransferStatus;
import com.ces.service.module.inventory.service.InventoryTransferService;
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

/**
 * Qovluqlar arası transfer.
 *
 * <p>No approval queue: receiving is itself the second pair of eyes, and whether that has to be a
 * different person is a branch setting.
 */
@RestController
@RequestMapping("/api/v1/inventory/transfers")
public class InventoryTransferController {

    private final InventoryTransferService transferService;

    public InventoryTransferController(InventoryTransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<TransferResponse>>> list(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransferResponse> result = transferService.list(
                status,
                PageRequest.of(
                        Math.max(page, 1) - 1,
                        Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "sentAt")));
        PageResponse<TransferResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<TransferResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<TransferResponse>> send(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(transferService.send(request)));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<TransferResponse>> receive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.receive(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<TransferResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.cancel(id)));
    }
}
