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
                .docNumber(request.getDocNumber())
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .fileName(stored.fileName())
                .fileUrl(stored.url())
                .fileSize(stored.size())
                .notes(request.getNotes())
                .build();
        document.setBranchId(branchId);
        VehicleDocument saved = documentRepository.save(document);
        auditLogger.log("CREATE", "VEHICLE_DOCUMENT", saved.getId(), null, VehicleDocumentResponse.from(saved));
        return VehicleDocumentResponse.from(saved);
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
