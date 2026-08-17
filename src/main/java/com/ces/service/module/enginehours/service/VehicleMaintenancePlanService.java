package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.enginehours.dto.VehicleMaintenancePlanRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenancePlanResponse;
import com.ces.service.module.enginehours.entity.VehicleMaintenancePlan;
import com.ces.service.module.enginehours.repository.VehicleMaintenancePlanRepository;
import com.ces.service.module.garage.entity.GarageMaintenanceTemplate;
import com.ces.service.module.garage.entity.GarageMaintenanceTemplateItem;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.GarageMaintenanceTemplateItemRepository;
import com.ces.service.module.garage.repository.GarageMaintenanceTemplateRepository;
import com.ces.service.module.garage.repository.VehicleRepository;
import com.ces.service.module.garage.service.GarageConfigService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A vehicle's own periodic-maintenance plan (SRS M03/M08 "Periodik texniki baxımlar"). Each line
 * is a self-contained copy — either cloned from a {@code GarageMaintenanceTemplate} via
 * {@link #applyTemplate} or added directly — so a later change to the source template never
 * reaches back into vehicles that already applied it (brief).
 */
@Service
@Transactional
public class VehicleMaintenancePlanService {

    private final VehicleMaintenancePlanRepository planRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageMaintenanceTemplateRepository templateRepository;
    private final GarageMaintenanceTemplateItemRepository templateItemRepository;
    private final GarageConfigService configService;
    private final MotosaatAuditLogger auditLogger;

    public VehicleMaintenancePlanService(
            VehicleMaintenancePlanRepository planRepository,
            VehicleRepository vehicleRepository,
            GarageMaintenanceTemplateRepository templateRepository,
            GarageMaintenanceTemplateItemRepository templateItemRepository,
            GarageConfigService configService,
            MotosaatAuditLogger auditLogger) {
        this.planRepository = planRepository;
        this.vehicleRepository = vehicleRepository;
        this.templateRepository = templateRepository;
        this.templateItemRepository = templateItemRepository;
        this.configService = configService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehicleMaintenancePlanResponse> list(UUID vehicleId) {
        Vehicle vehicle = loadVehicle(vehicleId);
        return planRepository.findByVehicleIdAndDeletedAtIsNullOrderByMaintenanceTypeAsc(vehicleId).stream()
                .map(p -> describe(p, vehicle))
                .collect(Collectors.toList());
    }

    public VehicleMaintenancePlanResponse create(UUID vehicleId, VehicleMaintenancePlanRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        assertHasInterval(request);
        configService.ensureRegistered(vehicle.getBranchId(), GarageConfigListType.MAINTENANCE_TYPE, request.getMaintenanceType());

        VehicleMaintenancePlan plan = VehicleMaintenancePlan.builder()
                .vehicleId(vehicleId)
                .maintenanceType(request.getMaintenanceType())
                .intervalMeterHours(request.getIntervalMeterHours())
                .intervalKm(request.getIntervalKm())
                .intervalCalendarDays(request.getIntervalCalendarDays())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .notes(request.getNotes())
                .build();
        plan.setBranchId(vehicle.getBranchId());
        // A freshly added plan's clock starts now by default — the vehicle's current
        // reading/today's date is the baseline the first "next due" is measured from, unless the
        // request names an explicit lastDone* (equipment whose last real service already
        // happened at a known, different value).
        seedBaseline(plan, vehicle, request);
        recomputeNextDue(plan);

        VehicleMaintenancePlan saved = planRepository.save(plan);
        VehicleMaintenancePlanResponse response = describe(saved, vehicle);
        auditLogger.log("CREATE", "MAINTENANCE_PLAN", saved.getId(), null, response);
        return response;
    }

    public VehicleMaintenancePlanResponse update(UUID vehicleId, UUID id, VehicleMaintenancePlanRequest request) {
        assertHasInterval(request);
        Vehicle vehicle = loadVehicle(vehicleId);
        VehicleMaintenancePlan plan = loadPlan(vehicleId, id);
        VehicleMaintenancePlanResponse before = describe(plan, vehicle);
        configService.ensureRegistered(vehicle.getBranchId(), GarageConfigListType.MAINTENANCE_TYPE, request.getMaintenanceType());

        plan.setMaintenanceType(request.getMaintenanceType());
        plan.setIntervalMeterHours(request.getIntervalMeterHours());
        plan.setIntervalKm(request.getIntervalKm());
        plan.setIntervalCalendarDays(request.getIntervalCalendarDays());
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }
        plan.setNotes(request.getNotes());
        // Interval changed but progress-so-far did not — only the next target moves.
        recomputeNextDue(plan);

        VehicleMaintenancePlanResponse after = describe(plan, vehicle);
        auditLogger.log("UPDATE", "MAINTENANCE_PLAN", id, before, after);
        return after;
    }

    public void delete(UUID vehicleId, UUID id) {
        Vehicle vehicle = loadVehicle(vehicleId);
        VehicleMaintenancePlan plan = loadPlan(vehicleId, id);
        VehicleMaintenancePlanResponse before = describe(plan, vehicle);
        plan.setDeletedAt(java.time.Instant.now());
        auditLogger.log("DELETE", "MAINTENANCE_PLAN", id, before, null);
    }

    /** Clones every active line of a matching-equipment-type template onto this vehicle. */
    public List<VehicleMaintenancePlanResponse> applyTemplate(UUID vehicleId, UUID templateId) {
        Vehicle vehicle = loadVehicle(vehicleId);
        GarageMaintenanceTemplate template = templateRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(templateId, vehicle.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance template not found: " + templateId));
        if (!template.getEquipmentType().equals(vehicle.getVehicleType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        List<String> existingTypes = planRepository
                .findByVehicleIdAndDeletedAtIsNullOrderByMaintenanceTypeAsc(vehicleId).stream()
                .filter(VehicleMaintenancePlan::getIsActive)
                .map(VehicleMaintenancePlan::getMaintenanceType)
                .collect(Collectors.toList());

        List<VehicleMaintenancePlanResponse> created = new ArrayList<>();
        for (GarageMaintenanceTemplateItem item : templateItemRepository
                .findByTemplateIdAndDeletedAtIsNullOrderBySortOrderAsc(templateId)) {
            // Re-applying the same template twice (or two templates that share a maintenance
            // type) shouldn't duplicate an already-active line.
            if (existingTypes.contains(item.getMaintenanceType())) {
                continue;
            }
            VehicleMaintenancePlan plan = VehicleMaintenancePlan.builder()
                    .vehicleId(vehicleId)
                    .maintenanceType(item.getMaintenanceType())
                    .intervalMeterHours(item.getIntervalMeterHours())
                    .intervalKm(item.getIntervalKm())
                    .intervalCalendarDays(item.getIntervalCalendarDays())
                    .sourceTemplateItemId(item.getId())
                    .isActive(true)
                    .notes(item.getNotes())
                    .build();
            plan.setBranchId(vehicle.getBranchId());
            seedBaseline(plan, vehicle);
            recomputeNextDue(plan);
            VehicleMaintenancePlan saved = planRepository.save(plan);
            created.add(describe(saved, vehicle));
        }
        auditLogger.log("BUSINESS", "MAINTENANCE_TEMPLATE_APPLIED", vehicleId, null, created);
        return created;
    }

    // ── package-private: used by VehicleMaintenanceCompletionService ───────

    /** Label lookup for completion history, tolerant of a plan later deleted or deactivated. */
    @Transactional(readOnly = true)
    String findMaintenanceTypeLabel(UUID planId) {
        return planRepository.findById(planId).map(VehicleMaintenancePlan::getMaintenanceType).orElse("—");
    }

    VehicleMaintenancePlan loadActivePlan(UUID vehicleId, UUID id) {
        VehicleMaintenancePlan plan = loadPlan(vehicleId, id);
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new BusinessException(ErrorCode.MAINTENANCE_PLAN_NOT_ACTIVE);
        }
        return plan;
    }

    void recomputeNextDue(VehicleMaintenancePlan plan) {
        plan.setNextDueEngineHours(plan.getIntervalMeterHours() != null && plan.getLastDoneEngineHours() != null
                ? plan.getLastDoneEngineHours().add(plan.getIntervalMeterHours()) : null);
        plan.setNextDueKm(plan.getIntervalKm() != null && plan.getLastDoneKm() != null
                ? plan.getLastDoneKm().add(plan.getIntervalKm()) : null);
        plan.setNextDueDate(plan.getIntervalCalendarDays() != null && plan.getLastDoneDate() != null
                ? plan.getLastDoneDate().plusDays(plan.getIntervalCalendarDays()) : null);
    }

    VehicleMaintenancePlanResponse describe(VehicleMaintenancePlan plan, Vehicle vehicle) {
        boolean dueByHours = plan.getNextDueEngineHours() != null && vehicle.getCurrentEngineHours() != null
                && vehicle.getCurrentEngineHours().compareTo(plan.getNextDueEngineHours()) >= 0;
        boolean dueByKm = plan.getNextDueKm() != null && vehicle.getCurrentKm() != null
                && vehicle.getCurrentKm().compareTo(plan.getNextDueKm()) >= 0;
        LocalDate today = LocalDate.now();
        boolean dueByCalendar = plan.getNextDueDate() != null && !today.isBefore(plan.getNextDueDate());

        BigDecimal overdueMeterAmount = null;
        if (dueByHours) {
            overdueMeterAmount = vehicle.getCurrentEngineHours().subtract(plan.getNextDueEngineHours());
        } else if (dueByKm) {
            overdueMeterAmount = vehicle.getCurrentKm().subtract(plan.getNextDueKm());
        }
        Integer overdueDays = dueByCalendar ? (int) ChronoUnit.DAYS.between(plan.getNextDueDate(), today) : null;

        return VehicleMaintenancePlanResponse.from(
                plan, dueByHours || dueByKm || dueByCalendar, overdueMeterAmount, overdueDays);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Template clones never carry a user-entered baseline — always the vehicle's current reading. */
    private void seedBaseline(VehicleMaintenancePlan plan, Vehicle vehicle) {
        seedBaseline(plan, vehicle, null);
    }

    private void seedBaseline(VehicleMaintenancePlan plan, Vehicle vehicle, VehicleMaintenancePlanRequest request) {
        if (plan.getIntervalMeterHours() != null) {
            BigDecimal override = request != null ? request.getLastDoneEngineHours() : null;
            plan.setLastDoneEngineHours(override != null
                    ? override
                    : (vehicle.getCurrentEngineHours() == null ? BigDecimal.ZERO : vehicle.getCurrentEngineHours()));
        }
        if (plan.getIntervalKm() != null) {
            BigDecimal override = request != null ? request.getLastDoneKm() : null;
            plan.setLastDoneKm(override != null
                    ? override
                    : (vehicle.getCurrentKm() == null ? BigDecimal.ZERO : vehicle.getCurrentKm()));
        }
        if (plan.getIntervalCalendarDays() != null) {
            LocalDate override = request != null ? request.getLastDoneDate() : null;
            plan.setLastDoneDate(override != null ? override : LocalDate.now());
        }
    }

    private void assertHasInterval(VehicleMaintenancePlanRequest request) {
        if (request.getIntervalMeterHours() == null && request.getIntervalKm() == null
                && request.getIntervalCalendarDays() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private VehicleMaintenancePlan loadPlan(UUID vehicleId, UUID id) {
        return planRepository.findByIdAndVehicleIdAndDeletedAtIsNull(id, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance plan not found: " + id));
    }

    private Vehicle loadVehicle(UUID vehicleId) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
