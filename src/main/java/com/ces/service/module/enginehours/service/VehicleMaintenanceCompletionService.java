package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.enginehours.dto.MaterialLineRequest;
import com.ces.service.module.enginehours.dto.MaterialLineResponse;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionRequest;
import com.ces.service.module.enginehours.dto.VehicleMaintenanceCompletionResponse;
import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletion;
import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletionMaterial;
import com.ces.service.module.enginehours.entity.VehicleMaintenancePlan;
import com.ces.service.module.enginehours.enums.MaterialKind;
import com.ces.service.module.enginehours.enums.MeterType;
import com.ces.service.module.enginehours.repository.VehicleMaintenanceCompletionMaterialRepository;
import com.ces.service.module.enginehours.repository.VehicleMaintenanceCompletionRepository;
import com.ces.service.module.garage.dto.VehicleComponentInstallRequest;
import com.ces.service.module.garage.dto.VehicleComponentResponse;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.repository.VehicleRepository;
import com.ces.service.module.garage.service.VehicleComponentService;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitUpdateRequest;
import com.ces.service.module.inventory.dto.StockQuantityRequest;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.service.InventoryItemService;
import com.ces.service.module.inventory.service.InventoryItemUnitService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Baxımı tamamla" — the manual completion path (brief). Meter values entered here are also
 * recorded as ordinary {@link com.ces.service.module.enginehours.entity.MeterReading} rows
 * (source "Baxım"), so the vehicle's cached current value and its meter history both move
 * together with the completion, and the plan's next-due target advances off the same numbers.
 *
 * <p>Materials used are optionally linked to real Anbar/Stok movements (see
 * {@link MaterialLineRequest}) — the first cross-module write from Motosaat into Inventory.
 * A consumable line submits an ordinary stock-out, reviewed through Anbar's own existing approval
 * queue exactly like any other stock-out; the completion itself is not held up waiting for it. A
 * serialized line marks the chosen unit IN_USE directly (unit status changes are not
 * approval-gated) and mirrors it into {@code vehicle_components} so "Komponentlər" stays in sync
 * without a separate manual step.
 */
@Service
@Transactional
public class VehicleMaintenanceCompletionService {

    private final VehicleMaintenanceCompletionRepository completionRepository;
    private final VehicleMaintenanceCompletionMaterialRepository materialRepository;
    private final VehicleMaintenancePlanService planService;
    private final MeterReadingService meterReadingService;
    private final VehicleRepository vehicleRepository;
    private final VehicleComponentService vehicleComponentService;
    private final InventoryItemService inventoryItemService;
    private final InventoryItemUnitService inventoryItemUnitService;
    private final ApprovalService approvalService;
    private final MotosaatAuditLogger auditLogger;

    public VehicleMaintenanceCompletionService(
            VehicleMaintenanceCompletionRepository completionRepository,
            VehicleMaintenanceCompletionMaterialRepository materialRepository,
            VehicleMaintenancePlanService planService,
            MeterReadingService meterReadingService,
            VehicleRepository vehicleRepository,
            VehicleComponentService vehicleComponentService,
            InventoryItemService inventoryItemService,
            InventoryItemUnitService inventoryItemUnitService,
            ApprovalService approvalService,
            MotosaatAuditLogger auditLogger) {
        this.completionRepository = completionRepository;
        this.materialRepository = materialRepository;
        this.planService = planService;
        this.meterReadingService = meterReadingService;
        this.vehicleRepository = vehicleRepository;
        this.vehicleComponentService = vehicleComponentService;
        this.inventoryItemService = inventoryItemService;
        this.inventoryItemUnitService = inventoryItemUnitService;
        this.approvalService = approvalService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<VehicleMaintenanceCompletionResponse> list(UUID vehicleId) {
        loadVehicle(vehicleId);
        List<VehicleMaintenanceCompletion> completions =
                completionRepository.findByVehicleIdAndDeletedAtIsNullOrderByCompletedAtDesc(vehicleId);
        Map<UUID, List<MaterialLineResponse>> materialsByCompletion = materialRepository
                .findByCompletionIdInAndDeletedAtIsNull(completions.stream().map(VehicleMaintenanceCompletion::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        VehicleMaintenanceCompletionMaterial::getCompletionId,
                        Collectors.mapping(MaterialLineResponse::from, Collectors.toList())));
        return completions.stream()
                .map(c -> VehicleMaintenanceCompletionResponse.from(
                        c, maintenanceTypeOf(c.getPlanId()), materialsByCompletion.getOrDefault(c.getId(), List.of())))
                .collect(Collectors.toList());
    }

    public VehicleMaintenanceCompletionResponse complete(
            UUID vehicleId, UUID planId, VehicleMaintenanceCompletionRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        VehicleMaintenancePlan plan = planService.loadActivePlan(vehicleId, planId);
        LocalDate completedAt = request.getCompletedAt() == null ? LocalDate.now() : request.getCompletedAt();
        List<MaterialLineRequest> materialLines = request.getMaterials() == null ? List.of() : request.getMaterials();
        materialLines.forEach(this::assertMaterialLineValid);

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

        List<MaterialLineResponse> materials = processMaterials(
                vehicle, saved.getId(), completedAt, request.getMeterEngineHours(), request.getMeterKm(), materialLines);

        VehicleMaintenanceCompletionResponse response =
                VehicleMaintenanceCompletionResponse.from(saved, plan.getMaintenanceType(), materials);
        auditLogger.log("BUSINESS", "MAINTENANCE_COMPLETED", plan.getId(), null, response);
        return response;
    }

    // ── materials ────────────────────────────────────────────────────────

    private void assertMaterialLineValid(MaterialLineRequest line) {
        boolean valid = switch (line.getKind()) {
            case CONSUMABLE -> line.getInventoryItemId() != null && line.getInventoryNodeId() != null
                    && line.getQuantity() != null;
            case SERIALIZED -> line.getInventoryUnitId() != null;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.MATERIAL_LINE_INVALID);
        }
    }

    private List<MaterialLineResponse> processMaterials(
            Vehicle vehicle,
            UUID completionId,
            LocalDate completedAt,
            BigDecimal meterEngineHours,
            BigDecimal meterKm,
            List<MaterialLineRequest> lines) {
        List<MaterialLineResponse> result = new ArrayList<>();
        for (MaterialLineRequest line : lines) {
            VehicleMaintenanceCompletionMaterial material = line.getKind() == MaterialKind.CONSUMABLE
                    ? processConsumable(vehicle, completionId, line)
                    : processSerialized(vehicle, completionId, completedAt, meterEngineHours, meterKm, line);
            material.setBranchId(vehicle.getBranchId());
            result.add(MaterialLineResponse.from(materialRepository.save(material)));
        }
        return result;
    }

    /** Submits an ordinary Anbar stock-out — reviewed independently, same as any other. */
    private VehicleMaintenanceCompletionMaterial processConsumable(
            Vehicle vehicle, UUID completionId, MaterialLineRequest line) {
        InventoryItemResponse item = inventoryItemService.get(line.getInventoryItemId());
        StockQuantityRequest stockRequest = StockQuantityRequest.builder()
                .nodeId(line.getInventoryNodeId())
                .quantity(line.getQuantity())
                .reason("Baxım materialı — " + vehicle.getCode())
                .build();
        ApprovalRequestResponse submitted = approvalService.submit(
                ApprovalEntityType.INVENTORY_ITEM, item.getId(), item.getName(),
                ApprovalOperation.STOCK_OUT, stockRequest, item);

        return VehicleMaintenanceCompletionMaterial.builder()
                .completionId(completionId)
                .kind(MaterialKind.CONSUMABLE)
                .inventoryItemId(item.getId())
                .inventoryItemName(item.getName())
                .inventoryNodeId(line.getInventoryNodeId())
                .quantity(line.getQuantity())
                .unit(item.getUnit())
                .stockApprovalRequestId(submitted.id())
                .notes(line.getNotes())
                .build();
    }

    /** Marks the unit IN_USE and mirrors it as a new ACTIVE Qaraj component — see class javadoc. */
    private VehicleMaintenanceCompletionMaterial processSerialized(
            Vehicle vehicle,
            UUID completionId,
            LocalDate completedAt,
            BigDecimal meterEngineHours,
            BigDecimal meterKm,
            MaterialLineRequest line) {
        InventoryItemUnitResponse unit = inventoryItemUnitService.get(line.getInventoryUnitId());
        if (unit.getStatus() != InventoryUnitStatus.IN_STOCK) {
            throw new BusinessException(ErrorCode.INVENTORY_UNIT_NOT_AVAILABLE);
        }
        inventoryItemUnitService.update(
                unit.getId(), InventoryItemUnitUpdateRequest.builder().status(InventoryUnitStatus.IN_USE).build());

        VehicleComponentResponse component = vehicleComponentService.install(
                vehicle.getId(),
                VehicleComponentInstallRequest.builder()
                        .componentType(unit.getItemName())
                        .identifier(unit.getSerialNumber())
                        .installedAt(completedAt)
                        .installedMeterValue(meterEngineHours != null ? meterEngineHours : meterKm)
                        .notes(line.getNotes())
                        .build());

        return VehicleMaintenanceCompletionMaterial.builder()
                .completionId(completionId)
                .kind(MaterialKind.SERIALIZED)
                .inventoryUnitId(unit.getId())
                .serialNumber(unit.getSerialNumber())
                .vehicleComponentId(component.getId())
                .notes(line.getNotes())
                .build();
    }

    private String maintenanceTypeOf(UUID planId) {
        return planService.findMaintenanceTypeLabel(planId);
    }

    private Vehicle loadVehicle(UUID vehicleId) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
