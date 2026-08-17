package com.ces.service.module.garage.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.garage.dto.VehicleDocumentRequest;
import com.ces.service.module.garage.dto.VehicleDocumentResponse;
import com.ces.service.module.garage.service.VehicleDocumentService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/documents")
public class VehicleDocumentController {

    private final VehicleDocumentService documentService;

    public VehicleDocumentController(VehicleDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<ApiResponse<List<VehicleDocumentResponse>>> list(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.list(vehicleId)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<VehicleDocumentResponse>> upload(
            @PathVariable UUID vehicleId,
            @Valid @ModelAttribute VehicleDocumentRequest request,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(documentService.upload(vehicleId, request, file)));
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public ResponseEntity<Resource> download(@PathVariable UUID vehicleId, @PathVariable UUID documentId) {
        VehicleDocumentService.DownloadableFile file = documentService.download(vehicleId, documentId);
        return ResponseEntity.ok()
                .contentType(file.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(file.resource());
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAuthority('VEHICLE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID vehicleId, @PathVariable UUID documentId) {
        documentService.delete(vehicleId, documentId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
