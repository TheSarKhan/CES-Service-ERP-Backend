package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.module.inventory.dto.InventoryUploadResponse;
import com.ces.service.module.inventory.service.InventoryUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Image uploads backing IMAGE / MULTI_IMAGE dynamic category fields. */
@RestController
@RequestMapping("/api/v1/inventory/uploads")
public class InventoryUploadController {

    private final InventoryUploadService uploadService;

    public InventoryUploadController(InventoryUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        String relativeUrl = uploadService.store(file);
        // Absolute so it resolves correctly regardless of which origin the frontend is served
        // from (they're separate origins in prod — a relative path would resolve against the
        // frontend's own host instead of the API's).
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new InventoryUploadResponse(absoluteUrl)));
    }
}
