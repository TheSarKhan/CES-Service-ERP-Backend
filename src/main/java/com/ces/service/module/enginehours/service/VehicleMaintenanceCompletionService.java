package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionResponse;
import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletion;
import com.ces.service.module.enginehours.entity.VehicleMaintenancePlan;
import com.ces.service.module.enginehours.enums.MeterType;
import com.ces.service.module.enginehours.repository.VehicleMaintenanceCompletionRepository;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.repository.VehicleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Baxımı tamamla" — the manual completion path (brief). Meter values entered here are also
 * recorded as ordinary {@link com.ces.service.module.enginehours.entity.MeterReading} rows
 * (source "Baxım"), so the vehicle's cached current value and its meter history both move
 * together with the completion, and the plan's next-due target advances off the same numbers.
 */
@Service
@Transactional
public class VehicleMaintenanceCompletionService {

    private final VehicleMaintenanceCompletionRepository completionRepository;
    private final VehicleMaintenancePlanService planService;
    private final MeterReadingService meterReadingService;
    private final VehicleRepository vehicleRepository;
    private final MotosaatAuditLogger auditLogger;

    public VehicleMaintenanceCompletionService(
            VehicleMaintenanceCompletionRepository completionRepository,
            VehicleMaintenancePlanService planService,
            MeterReadingService meterReadingService,
            VehicleRepository vehicleRepository,
            MotosaatAuditLogger auditLogger) {
        this.completionRepository = completionRepository;
        this.planService = planService;
        this.meterReadingService = meterReadingService;
        this.vehicleRepository = vehicleRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehicleMaintenanceCompletionResponse> list(UUID vehicleId) {
        loadVehicle(vehicleId);
        return completionRepository.findByVehicleIdAndDeletedAtIsNullOrderByCompletedAtDesc(vehicleId).stream()
                .map(c -> VehicleMaintenanceCompletionResponse.from(c, maintenanceTypeOf(c.getPlanId())))
                .collect(Collectors.toList());
    }

    public VehicleMaintenanceCompletionResponse complete(
            UUID vehicleId, UUID planId, VehicleMaintenanceCompletionRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        VehicleMaintenancePlan plan = planService.loadActivePlan(vehicleId, planId);
        LocalDate completedAt = request.getCompletedAt() == null ? LocalDate.now() : request.getCompletedAt();

        if (request.getMeterEngineHours() != null) {
            if (!Boolean.TRUE.equals(vehicle.getUsesEngineHours())) {
                throw new BusinessException(ErrorCode.METER_TYPE_NOT_ENABLED);
            }
        }
        if (request.getMeterKm() != null && !Boolean.TRUE.equals(vehicle.getUsesKm())) {
            throw new BusinessException(ErrorCode.METER_TYPE_NOT_ENABLED);
        }

        VehicleMaintenanceCompletion completion = VehicleMaintenanceCompletion.builder()
                .planId(planId)
                .vehicleId(vehicleId)
                .completedAt(completedAt)
                .meterEngineHours(request.getMeterEngineHours())
                .meterKm(request.getMeterKm())
                .description(request.getDescription())
                .materialsNotes(request.getMaterialsNotes())
                .notes(request.getNotes())
                .build();
        completion.setBranchId(vehicle.getBranchId());
        VehicleMaintenanceCompletion saved = completionRepository.save(completion);

        if (request.getMeterEngineHours() != null) {
            meterReadingService.recordFromSource(
                    vehicleId, MeterType.ENGINE_HOURS, request.getMeterEngineHours(), completedAt, "Baxım", saved.getId());
            plan.setLastDoneEngineHours(request.getMeterEngineHours());
        }
        if (request.getMeterKm() != null) {
            meterReadingService.recordFromSource(
                    vehicleId, MeterType.KILOMETERS, request.getMeterKm(), completedAt, "Baxım", saved.getId());
            plan.setLastDoneKm(request.getMeterKm());
        }
        plan.setLastDoneDate(completedAt);
        planService.recomputeNextDue(plan);

        VehicleMaintenanceCompletionResponse response =
                VehicleMaintenanceCompletionResponse.from(saved, plan.getMaintenanceType());
        auditLogger.log("BUSINESS", "MAINTENANCE_COMPLETED", plan.getId(), null, response);
        return response;
    }

    private String maintenanceTypeOf(UUID planId) {
        return planService.findMaintenanceTypeLabel(planId);
    }

    private Vehicle loadVehicle(UUID vehicleId) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
