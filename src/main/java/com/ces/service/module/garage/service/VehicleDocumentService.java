package com.ces.service.module.garage.service;

import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.VehicleDocumentRequest;
import com.ces.service.module.garage.dto.VehicleDocumentResponse;
import com.ces.service.module.garage.entity.VehicleDocument;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.VehicleDocumentRepository;
import com.ces.service.module.garage.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documents filed against a vehicle. Direct CRUD, not routed through approval — the user's
 * approval decision was scoped to the vehicle's own identity fields (name/status/owner), not its
 * attachments, matching how Inventory treats stock movements versus category structure.
 */
@Service
@Transactional
public class VehicleDocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageConfigService configService;
    private final GarageUploadService uploadService;
    private final GarageAuditLogger auditLogger;

    public VehicleDocumentService(
            VehicleDocumentRepository documentRepository,
            VehicleRepository vehicleRepository,
            GarageConfigService configService,
            GarageUploadService uploadService,
            GarageAuditLogger auditLogger) {
        this.documentRepository = documentRepository;
        this.vehicleRepository = vehicleRepository;
        this.configService = configService;
        this.uploadService = uploadService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentResponse> list(UUID vehicleId) {
        assertVehicleExists(vehicleId);
        return documentRepository.findByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(vehicleId).stream()
                .map(VehicleDocumentResponse::from)
                .collect(Collectors.toList());
    }

    public VehicleDocumentResponse upload(UUID vehicleId, VehicleDocumentRequest request, MultipartFile file) {
        UUID branchId = BranchContext.get();
        assertVehicleExists(vehicleId);
        // Ad-hoc: a document type typed for the first time becomes a reusable choice, per the brief.
        configService.ensureRegistered(branchId, GarageConfigListType.DOC_TYPE, request.getDocType());

        GarageUploadService.StoredFile stored = uploadService.store(file);
        VehicleDocument document = VehicleDocument.builder()
                .vehicleId(vehicleId)
                .docType(request.getDocType())
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .fileName(stored.fileName())
                .fileUrl(stored.url())
                .fileSize(stored.size())
                .notes(request.getNotes())
                .build();
        document.setBranchId(branchId);
        // flush, not save: @Generated only refreshes `docNumber` once the INSERT actually reaches
        // the database and the trigger has run — see Vehicle.create()'s identical reasoning.
        VehicleDocument saved = documentRepository.saveAndFlush(document);
        auditLogger.log("CREATE", "VEHICLE_DOCUMENT", saved.getId(), null, VehicleDocumentResponse.from(saved));
        return VehicleDocumentResponse.from(saved);
    }

    public record DownloadableFile(Resource resource, String fileName, MediaType contentType) {
    }

    /**
     * Streams the file back with its original name — unlike the public static handler (see
     * {@code UploadStaticResourceConfig}), which serves the same bytes under the opaque stored
     * UUID filename and sets no {@code Content-Disposition} at all.
     */
    @Transactional(readOnly = true)
    public DownloadableFile download(UUID vehicleId, UUID documentId) {
        VehicleDocument document = documentRepository.findByIdAndVehicleIdAndDeletedAtIsNull(documentId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle document not found: " + documentId));
        Resource resource = uploadService.loadAsResource(document.getFileUrl());
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return new DownloadableFile(resource, document.getFileName(), contentType);
    }

    public void delete(UUID vehicleId, UUID documentId) {
        VehicleDocument document = documentRepository.findByIdAndVehicleIdAndDeletedAtIsNull(documentId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle document not found: " + documentId));
        document.setDeletedAt(Instant.now());
        auditLogger.log("DELETE", "VEHICLE_DOCUMENT", documentId, VehicleDocumentResponse.from(document), null);
    }

    private void assertVehicleExists(UUID vehicleId) {
        vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
