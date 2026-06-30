package com.ces.service.module.vehicle.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.vehicle.dto.VehicleDocumentRequest;
import com.ces.service.module.vehicle.dto.VehicleDocumentResponse;
import com.ces.service.module.vehicle.dto.VehicleRequest;
import com.ces.service.module.vehicle.dto.VehicleResponse;
import com.ces.service.module.vehicle.dto.VehicleStatusRequest;
import com.ces.service.module.vehicle.enums.GarageType;
import com.ces.service.module.vehicle.enums.VehicleStatus;
import com.ces.service.module.vehicle.service.VehicleService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vehicle (Garage) endpoints (SRS M03.2). */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> list(
            @RequestParam(name = "garage_type", required = false) GarageType garageType,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<VehicleResponse> result = vehicleService.list(garageType, status, make, search, pageable);
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
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody VehicleStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.changeStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<VehicleDocumentResponse>>> listDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.listDocuments(id)));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehicleDocumentResponse>> addDocument(
            @PathVariable UUID id, @Valid @RequestBody VehicleDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(vehicleService.addDocument(id, request)));
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id, @PathVariable UUID docId) {
        vehicleService.deleteDocument(id, docId);
        return ResponseEntity.noContent().build();
    }

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sort));
    }
}
