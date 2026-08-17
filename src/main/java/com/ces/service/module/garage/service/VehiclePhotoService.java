package com.ces.service.module.garage.service;

import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.VehiclePhotoResponse;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.entity.VehiclePhoto;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.VehiclePhotoRepository;
import com.ces.service.module.garage.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Categorized photos. Direct CRUD — see {@link VehicleDocumentService} for why. */
@Service
@Transactional
public class VehiclePhotoService {

    private final VehiclePhotoRepository photoRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageConfigService configService;
    private final GarageUploadService uploadService;
    private final GarageAuditLogger auditLogger;

    public VehiclePhotoService(
            VehiclePhotoRepository photoRepository,
            VehicleRepository vehicleRepository,
            GarageConfigService configService,
            GarageUploadService uploadService,
            GarageAuditLogger auditLogger) {
        this.photoRepository = photoRepository;
        this.vehicleRepository = vehicleRepository;
        this.configService = configService;
        this.uploadService = uploadService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehiclePhotoResponse> list(UUID vehicleId) {
        assertVehicleExists(vehicleId);
        return photoRepository.findByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(vehicleId).stream()
                .map(VehiclePhotoResponse::from)
                .collect(Collectors.toList());
    }

    public VehiclePhotoResponse upload(UUID vehicleId, String category, String notes, MultipartFile file) {
        UUID branchId = BranchContext.get();
        Vehicle vehicle = loadVehicle(vehicleId);
        configService.ensureRegistered(branchId, GarageConfigListType.PHOTO_CATEGORY, category);

        GarageUploadService.StoredFile stored = uploadService.store(file);
        VehiclePhoto photo = VehiclePhoto.builder()
                .vehicleId(vehicleId)
                .category(category)
                .fileName(stored.fileName())
                .fileUrl(stored.url())
                .fileSize(stored.size())
                .notes(notes)
                .build();
        photo.setBranchId(branchId);
        VehiclePhoto saved = photoRepository.save(photo);

        // First photo on a vehicle becomes its cover automatically — otherwise the Texnikalar
        // list thumbnail (and anything else reading primaryPhotoUrl) stays on the generic
        // placeholder icon until someone remembers to click "Əsas foto et" by hand.
        if (vehicle.getPrimaryPhotoId() == null) {
            vehicle.setPrimaryPhotoId(saved.getId());
            vehicle.setPrimaryPhotoUrl(saved.getFileUrl());
        }

        auditLogger.log("CREATE", "VEHICLE_PHOTO", saved.getId(), null, VehiclePhotoResponse.from(saved));
        return VehiclePhotoResponse.from(saved);
    }

    public void delete(UUID vehicleId, UUID photoId) {
        VehiclePhoto photo = photoRepository.findByIdAndVehicleIdAndDeletedAtIsNull(photoId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle photo not found: " + photoId));
        photo.setDeletedAt(Instant.now());
        auditLogger.log("DELETE", "VEHICLE_PHOTO", photoId, VehiclePhotoResponse.from(photo), null);

        // The FK's ON DELETE SET NULL only clears primary_photo_id at the DB level — the
        // denormalized URL cache is application-owned and needs the same clearing here.
        Vehicle vehicle = loadVehicle(vehicleId);
        if (photoId.equals(vehicle.getPrimaryPhotoId())) {
            vehicle.setPrimaryPhotoId(null);
            vehicle.setPrimaryPhotoUrl(null);
        }
    }

    /** The cover photo shown in list views — see {@code Vehicle.primaryPhotoId}'s javadoc. */
    public void setPrimary(UUID vehicleId, UUID photoId) {
        VehiclePhoto photo = photoRepository.findByIdAndVehicleIdAndDeletedAtIsNull(photoId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle photo not found: " + photoId));
        Vehicle vehicle = loadVehicle(vehicleId);
        vehicle.setPrimaryPhotoId(photo.getId());
        vehicle.setPrimaryPhotoUrl(photo.getFileUrl());
    }

    private void assertVehicleExists(UUID vehicleId) {
        loadVehicle(vehicleId);
    }

    private Vehicle loadVehicle(UUID vehicleId) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
