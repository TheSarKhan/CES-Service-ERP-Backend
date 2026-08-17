package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.garage.dto.VehicleRequest;
import com.ces.service.module.garage.dto.VehicleResponse;
import com.ces.service.module.garage.enums.GarageType;
import com.ces.service.module.garage.service.VehicleService;
import com.ces.service.module.garageapproval.dto.GarageApprovalRequestResponse;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalOperation;
import com.ces.service.module.garageapproval.service.GarageApprovalService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
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

/**
 * Equipment (Texnika) endpoints — Qaraj.
 *
 * <p>Creating a vehicle applies immediately; editing its identity or removing it is deferred to
 * the Təsdiqləmələr queue, per the user's explicit choice for this module (see
 * {@code backend/docs/qaraj-motosaat-plani.md}).
 */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    /** Columns the equipment list may be ordered by; anything else falls back to the default. */
    private static final Set<String> SORTABLE =
            Set.of("code", "name", "make", "model", "status", "vehicleType", "createdAt");

    private final VehicleService vehicleService;
    private final GarageApprovalService approvalService;

    public VehicleController(VehicleService vehicleService, GarageApprovalService approvalService) {
        this.vehicleService = vehicleService;
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> list(
            @RequestParam(required = false) List<GarageType> garageType,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> vehicleType,
            @RequestParam(required = false) List<String> make,
            @RequestParam(required = false) List<String> model,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) Boolean usesEngineHours,
            @RequestParam(required = false) Boolean usesKm,
            @RequestParam(required = false) BigDecimal purchasePriceMin,
            @RequestParam(required = false) BigDecimal purchasePriceMax,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Page<VehicleResponse> result = vehicleService.list(
                garageType, status, vehicleType, make, model, location, ownerId,
                usesEngineHours, usesKm, purchasePriceMin, purchasePriceMax, search,
                toPageable(page, size, sort, dir));
        PageResponse<VehicleResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<VehicleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICLE_CREATE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(vehicleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        vehicleService.assertUpdateValid(id, request);
        return accepted(submit(id, GarageApprovalOperation.UPDATE, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_DELETE')")
    public ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> delete(@PathVariable UUID id) {
        return accepted(submit(id, GarageApprovalOperation.DELETE, null));
    }

    private GarageApprovalRequestResponse submit(UUID id, GarageApprovalOperation operation, Object payload) {
        VehicleResponse before = vehicleService.get(id);
        return approvalService.submit(
                GarageApprovalEntityType.VEHICLE, id, before.getName(), operation, payload, before);
    }

    private ResponseEntity<ApiResponse<GarageApprovalRequestResponse>> accepted(GarageApprovalRequestResponse request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(request));
    }

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        String field = SORTABLE.contains(sort) ? sort : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, field));
    }
}
