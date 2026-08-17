package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.VehiclePhotoResponse;
import com.ces.service.module.garage.service.VehiclePhotoService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/photos")
public class VehiclePhotoController {

    private final VehiclePhotoService photoService;

    public VehiclePhotoController(VehiclePhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<VehiclePhotoResponse>>> list(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.list(vehicleId)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehiclePhotoResponse>> upload(
            @PathVariable UUID vehicleId,
            @RequestParam String category,
            @RequestParam(required = false) String notes,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(photoService.upload(vehicleId, category, notes, file)));
    }

    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID vehicleId, @PathVariable UUID photoId) {
        photoService.delete(vehicleId, photoId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{photoId}/primary")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> setPrimary(@PathVariable UUID vehicleId, @PathVariable UUID photoId) {
        photoService.setPrimary(vehicleId, photoId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
