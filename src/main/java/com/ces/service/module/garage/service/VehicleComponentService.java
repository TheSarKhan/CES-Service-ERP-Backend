package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.VehicleComponentInstallRequest;
import com.ces.service.module.garage.dto.VehicleComponentRemoveRequest;
import com.ces.service.module.garage.dto.VehicleComponentResponse;
import com.ces.service.module.garage.entity.VehicleComponent;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.enums.VehicleComponentStatus;
import com.ces.service.module.garage.repository.VehicleComponentRepository;
import com.ces.service.module.garage.repository.VehicleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component swaps (mühərrik, hidravlik nasos, ...). No separate history table — see
 * {@link VehicleComponent}'s javadoc. Direct CRUD, not routed through approval; see
 * {@link VehicleDocumentService} for why.
 */
@Service
@Transactional
public class VehicleComponentService {

    private final VehicleComponentRepository componentRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageConfigService configService;
    private final GarageAuditLogger auditLogger;

    public VehicleComponentService(
            VehicleComponentRepository componentRepository,
            VehicleRepository vehicleRepository,
            GarageConfigService configService,
            GarageAuditLogger auditLogger) {
        this.componentRepository = componentRepository;
        this.vehicleRepository = vehicleRepository;
        this.configService = configService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehicleComponentResponse> list(UUID vehicleId) {
        assertVehicleExists(vehicleId);
        return componentRepository.findByVehicleIdAndDeletedAtIsNullOrderByInstalledAtDesc(vehicleId).stream()
                .map(VehicleComponentResponse::from)
                .collect(Collectors.toList());
    }

    public VehicleComponentResponse install(UUID vehicleId, VehicleComponentInstallRequest request) {
        UUID branchId = BranchContext.get();
        assertVehicleExists(vehicleId);
        configService.ensureRegistered(branchId, GarageConfigListType.COMPONENT_TYPE, request.getComponentType());

        VehicleComponent component = VehicleComponent.builder()
                .vehicleId(vehicleId)
                .componentType(request.getComponentType())
                .identifier(request.getIdentifier())
                .status(VehicleComponentStatus.ACTIVE)
                .installedAt(request.getInstalledAt() != null ? request.getInstalledAt() : LocalDate.now())
                .installedMeterValue(request.getInstalledMeterValue())
                .notes(request.getNotes())
                .build();
        component.setBranchId(branchId);
        VehicleComponent saved = componentRepository.save(component);
        auditLogger.log("CREATE", "VEHICLE_COMPONENT", saved.getId(), null, VehicleComponentResponse.from(saved));
        return VehicleComponentResponse.from(saved);
    }

    /** Marks a component REMOVED — its row stays, closing out its own slice of the history. */
    public VehicleComponentResponse remove(UUID vehicleId, UUID componentId, VehicleComponentRemoveRequest request) {
        VehicleComponent component = loadComponent(vehicleId, componentId);
        if (component.getStatus() != VehicleComponentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GARAGE_COMPONENT_NOT_ACTIVE);
        }
        VehicleComponentResponse before = VehicleComponentResponse.from(component);

        component.setStatus(VehicleComponentStatus.REMOVED);
        component.setRemovedAt(request.getRemovedAt() != null ? request.getRemovedAt() : LocalDate.now());
        component.setRemovedMeterValue(request.getRemovedMeterValue());
        component.setRemovalReason(request.getReason());

        VehicleComponentResponse after = VehicleComponentResponse.from(component);
        auditLogger.log("UPDATE", "VEHICLE_COMPONENT", componentId, before, after);
        return after;
    }

    private VehicleComponent loadComponent(UUID vehicleId, UUID componentId) {
        return componentRepository.findByIdAndVehicleIdAndDeletedAtIsNull(componentId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle component not found: " + componentId));
    }

    private void assertVehicleExists(UUID vehicleId) {
        vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
