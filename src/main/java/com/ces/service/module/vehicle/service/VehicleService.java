package com.ces.service.module.vehicle.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.vehicle.dto.VehicleDocumentRequest;
import com.ces.service.module.vehicle.dto.VehicleDocumentResponse;
import com.ces.service.module.vehicle.dto.VehicleRequest;
import com.ces.service.module.vehicle.dto.VehicleResponse;
import com.ces.service.module.vehicle.dto.VehicleStatusRequest;
import com.ces.service.module.vehicle.entity.Vehicle;
import com.ces.service.module.vehicle.entity.VehicleDocument;
import com.ces.service.module.vehicle.enums.GarageType;
import com.ces.service.module.vehicle.enums.VehicleStatus;
import com.ces.service.module.vehicle.mapper.VehicleMapper;
import com.ces.service.module.vehicle.repository.VehicleDocumentRepository;
import com.ces.service.module.vehicle.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vehicle management (M03). Reference CRUD module demonstrating the standard branch-scoped pattern.
 *
 * <p>Business rules (M03.3):
 * <ul>
 *   <li>chassis_number unique across all branches → {@link ErrorCode#DUPLICATE_CHASSIS_NUMBER}.</li>
 *   <li>garage_type INVESTOR / CUSTOMER require {@code owner_id} → {@link ErrorCode#OWNER_REQUIRED}.</li>
 *   <li>soft delete blocked when the vehicle has an active Work Order →
 *       {@link ErrorCode#VEHICLE_HAS_ACTIVE_WO} (WO module not built — guard stubbed).</li>
 * </ul>
 */
@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehicleDocumentRepository vehicleDocumentRepository,
            VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> list(
            GarageType garageType, VehicleStatus status, String make, String search, Pageable pageable) {
        UUID branchId = BranchContext.get();
        String normalizedMake = blankToNull(make);
        String normalizedSearch = blankToNull(search);
        Page<Vehicle> page = vehicleRepository.search(
                branchId, garageType, status, normalizedMake, normalizedSearch, pageable);
        return page.map(vehicleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID id) {
        return vehicleMapper.toResponse(loadVehicle(id));
    }

    public VehicleResponse create(VehicleRequest request) {
        UUID branchId = BranchContext.get();
        validateOwner(request.getGarageType(), request.getOwnerId());

        // chassis_number unique across all branches.
        if (request.getChassisNumber() != null
                && !request.getChassisNumber().isBlank()
                && vehicleRepository.existsByChassisNumberAndDeletedAtIsNull(request.getChassisNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CHASSIS_NUMBER);
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setBranchId(branchId);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    public VehicleResponse update(UUID id, VehicleRequest request) {
        Vehicle vehicle = loadVehicle(id);
        validateOwner(request.getGarageType(), request.getOwnerId());

        // chassis_number unique across all branches (excluding self).
        if (request.getChassisNumber() != null
                && !request.getChassisNumber().isBlank()
                && vehicleRepository.existsByChassisNumberAndDeletedAtIsNullAndIdNot(
                        request.getChassisNumber(), vehicle.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CHASSIS_NUMBER);
        }

        vehicleMapper.updateEntity(request, vehicle);
        // garageType is @NotNull on request; apply explicitly since updateEntity uses IGNORE strategy.
        vehicle.setGarageType(request.getGarageType());
        vehicle.setOwnerId(request.getOwnerId());
        return vehicleMapper.toResponse(vehicle);
    }

    public VehicleResponse changeStatus(UUID id, VehicleStatusRequest request) {
        Vehicle vehicle = loadVehicle(id);
        vehicle.setStatus(request.getStatus());
        if (request.getNote() != null && !request.getNote().isBlank()) {
            vehicle.setNotes(request.getNote());
        }
        return vehicleMapper.toResponse(vehicle);
    }

    public void delete(UUID id) {
        Vehicle vehicle = loadVehicle(id);

        // Soft delete blocked if the vehicle has an active Work Order (M03.3).
        if (hasActiveWorkOrder(vehicle.getId())) {
            throw new BusinessException(ErrorCode.VEHICLE_HAS_ACTIVE_WO);
        }

        vehicle.setDeletedAt(Instant.now());
        vehicleRepository.save(vehicle);
    }

    // ── documents ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VehicleDocumentResponse> listDocuments(UUID vehicleId) {
        UUID branchId = BranchContext.get();
        loadVehicle(vehicleId); // ensure visible in branch
        return vehicleDocumentRepository
                .findByVehicleIdAndBranchIdAndDeletedAtIsNull(vehicleId, branchId)
                .stream()
                .map(vehicleMapper::toDocumentResponse)
                .collect(Collectors.toList());
    }

    public VehicleDocumentResponse addDocument(UUID vehicleId, VehicleDocumentRequest request) {
        UUID branchId = BranchContext.get();
        loadVehicle(vehicleId);
        VehicleDocument document = vehicleMapper.toDocumentEntity(request);
        document.setVehicleId(vehicleId);
        document.setBranchId(branchId);
        VehicleDocument saved = vehicleDocumentRepository.save(document);
        return vehicleMapper.toDocumentResponse(saved);
    }

    public void deleteDocument(UUID vehicleId, UUID documentId) {
        UUID branchId = BranchContext.get();
        VehicleDocument document = vehicleDocumentRepository
                .findByIdAndVehicleIdAndBranchIdAndDeletedAtIsNull(documentId, vehicleId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle document not found: " + documentId));
        document.setDeletedAt(Instant.now());
        vehicleDocumentRepository.save(document);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void validateOwner(GarageType garageType, UUID ownerId) {
        if ((garageType == GarageType.INVESTOR || garageType == GarageType.CUSTOMER) && ownerId == null) {
            throw new BusinessException(ErrorCode.OWNER_REQUIRED);
        }
    }

    /**
     * M03.3 guard stub for "vehicle has active Work Order". The Work Order module (M06) is not yet
     * built; wire this to the WO repository when available.
     */
    private boolean hasActiveWorkOrder(UUID vehicleId) {
        // TODO(M06): integrate WorkOrderRepository.existsActiveByVehicleId(vehicleId) when available.
        return false;
    }

    private Vehicle loadVehicle(UUID id) {
        UUID branchId = BranchContext.get();
        return vehicleRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
