package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.VehicleParameterItem;
import com.ces.service.module.garage.dto.VehicleRequest;
import com.ces.service.module.garage.dto.VehicleResponse;
import com.ces.service.module.garage.entity.GarageConfigValue;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.enums.GarageType;
import com.ces.service.module.garage.repository.VehicleRepository;
import com.ces.service.module.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipment (Texnika) registry — the single source of truth every other module is meant to
 * reference rather than duplicate (SRS M03 / Qaraj brief).
 *
 * <p>Creating a vehicle is exempt from approval, matching how Inventory items work: there is
 * nothing to review yet, only a new record. Editing its identity — name, status, owner, core
 * identification numbers — goes through the approval queue instead, per the user's explicit
 * choice for this module (unlike the SRS, which does not require it).
 */
@Service
@Transactional
public class VehicleService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VehicleRepository vehicleRepository;
    private final GarageConfigService configService;
    private final GarageAuditLogger auditLogger;
    private final CustomerRepository customerRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            GarageConfigService configService,
            GarageAuditLogger auditLogger,
            CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.configService = configService;
        this.auditLogger = auditLogger;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> list(
            List<GarageType> garageType,
            List<String> status,
            List<String> vehicleType,
            List<String> make,
            List<String> model,
            String location,
            UUID ownerId,
            Boolean usesEngineHours,
            Boolean usesKm,
            BigDecimal purchasePriceMin,
            BigDecimal purchasePriceMax,
            String search,
            Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<Vehicle> page = vehicleRepository.search(
                branchId,
                emptyToNull(garageType),
                emptyToNull(status),
                emptyToNull(vehicleType),
                emptyToNull(make),
                emptyToNull(model),
                location,
                ownerId,
                usesEngineHours,
                usesKm,
                purchasePriceMin,
                purchasePriceMax,
                search,
                pageable);
        return page.map(VehicleResponse::from);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID id) {
        Vehicle vehicle = loadVehicle(id);
        String ownerName = vehicle.getOwnerId() == null
                ? null
                : customerRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicle.getOwnerId(), vehicle.getBranchId())
                        .map(c -> c.getFullName())
                        .orElse(null);
        return VehicleResponse.from(vehicle, ownerName);
    }

    public VehicleResponse create(VehicleRequest request) {
        UUID branchId = BranchContext.get();
        assertIdentifiersFree(null, request);

        Vehicle vehicle = Vehicle.builder()
                .name(request.getName())
                .garageType(request.getGarageType())
                .vehicleType(request.getVehicleType())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .chassisNumber(blankToNull(request.getChassisNumber()))
                .serialNumber(blankToNull(request.getSerialNumber()))
                .plateNumber(blankToNull(request.getPlateNumber()))
                .currentLocation(request.getCurrentLocation())
                .notes(request.getNotes())
                .purchaseDate(request.getPurchaseDate())
                .purchasePrice(request.getPurchasePrice())
                .marketValue(request.getMarketValue())
                .depreciationPercent(request.getDepreciationPercent())
                .safetyEquipment(nullToEmpty(request.getSafetyEquipment()))
                .mandatoryDocuments(nullToEmpty(request.getMandatoryDocuments()))
                .parameters(toJson(request.getParameters()))
                .build();
        vehicle.setBranchId(branchId);
        applyOwnership(vehicle, request);
        applyStatus(vehicle, branchId, request.getStatus());
        applyMeterUsage(vehicle, branchId, request);
        applyInitialMeterValue(vehicle, request);
        registerOpenValues(branchId, request);

        // flush, not save: @Generated only refreshes `code` once the INSERT actually reaches the
        // database and the trigger has run — JPA's default write-behind would otherwise defer that
        // until transaction commit, well after the response below is built from a still-null code.
        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        VehicleResponse response = VehicleResponse.from(saved);
        auditLogger.log("CREATE", "VEHICLE", saved.getId(), null, response);
        return response;
    }

    /**
     * Validates an update before the controller parks it for approval. A pending request locks
     * the vehicle, so a change that could never be applied — an owner-less CUSTOMER vehicle, a
     * duplicate plate number — has to be refused while the requester is still on the screen,
     * the same reasoning {@code InventoryItemService.assertMovable} uses.
     */
    @Transactional(readOnly = true)
    public void assertUpdateValid(UUID id, VehicleRequest request) {
        Vehicle vehicle = loadVehicle(id);
        assertIdentifiersFree(vehicle.getId(), request);
        validateOwnership(request);
        if (request.getStatus() != null) {
            configService.assertActiveValue(vehicle.getBranchId(), GarageConfigListType.STATUS, request.getStatus());
        }
    }

    /** Replays an approved update. Called only by {@link VehicleApprovalExecutor}. */
    public VehicleResponse applyUpdate(UUID id, VehicleRequest request) {
        Vehicle vehicle = loadVehicle(id);
        VehicleResponse before = VehicleResponse.from(vehicle);

        vehicle.setName(request.getName());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setChassisNumber(blankToNull(request.getChassisNumber()));
        vehicle.setSerialNumber(blankToNull(request.getSerialNumber()));
        vehicle.setPlateNumber(blankToNull(request.getPlateNumber()));
        vehicle.setCurrentLocation(request.getCurrentLocation());
        vehicle.setNotes(request.getNotes());
        vehicle.setPurchaseDate(request.getPurchaseDate());
        vehicle.setPurchasePrice(request.getPurchasePrice());
        vehicle.setMarketValue(request.getMarketValue());
        vehicle.setDepreciationPercent(request.getDepreciationPercent());
        vehicle.setSafetyEquipment(nullToEmpty(request.getSafetyEquipment()));
        vehicle.setMandatoryDocuments(nullToEmpty(request.getMandatoryDocuments()));
        vehicle.setParameters(toJson(request.getParameters()));
        applyOwnership(vehicle, request);
        applyStatus(vehicle, vehicle.getBranchId(), request.getStatus());
        applyMeterUsage(vehicle, vehicle.getBranchId(), request);
        registerOpenValues(vehicle.getBranchId(), request);

        VehicleResponse after = VehicleResponse.from(vehicle);
        auditLogger.log("UPDATE", "VEHICLE", id, before, after);
        return after;
    }

    /** Replays an approved deletion. Called only by {@link VehicleApprovalExecutor}. */
    public void applyDelete(UUID id) {
        Vehicle vehicle = loadVehicle(id);
        // Hard delete is never offered — see the class javadoc on why: service/maintenance/engine-
        // hour/warehouse-issue history all point at this row.
        vehicle.setDeletedAt(Instant.now());
        auditLogger.log("DELETE", "VEHICLE", id, VehicleResponse.from(vehicle), null);
    }

    // ── validation & derivation helpers ─────────────────────────────────────

    private void assertIdentifiersFree(UUID excludeId, VehicleRequest request) {
        assertIdentifierFree(excludeId, blankToNull(request.getChassisNumber()),
                excludeId == null ? vehicleRepository::existsByChassisNumberAndDeletedAtIsNull
                        : v -> vehicleRepository.existsByChassisNumberAndDeletedAtIsNullAndIdNot(v, excludeId));
        assertIdentifierFree(excludeId, blankToNull(request.getSerialNumber()),
                excludeId == null ? vehicleRepository::existsBySerialNumberAndDeletedAtIsNull
                        : v -> vehicleRepository.existsBySerialNumberAndDeletedAtIsNullAndIdNot(v, excludeId));
        assertIdentifierFree(excludeId, blankToNull(request.getPlateNumber()),
                excludeId == null ? vehicleRepository::existsByPlateNumberAndDeletedAtIsNull
                        : v -> vehicleRepository.existsByPlateNumberAndDeletedAtIsNullAndIdNot(v, excludeId));
    }

    private void assertIdentifierFree(UUID excludeId, String value, java.util.function.Predicate<String> exists) {
        if (value != null && exists.test(value)) {
            throw new BusinessException(ErrorCode.GARAGE_IDENTIFIER_DUPLICATE);
        }
    }

    private void validateOwnership(VehicleRequest request) {
        if (request.getGarageType() == GarageType.CUSTOMER && request.getOwnerId() == null) {
            throw new BusinessException(ErrorCode.GARAGE_OWNER_REQUIRED);
        }
        // ownerId's existence in `customers` is left to the FK constraint: Customers (M04) has no
        // Java module yet, so there is no repository here to check against cleanly. Once it
        // exists, this is the place to validate ownership.eagerly.
    }

    private void applyOwnership(Vehicle vehicle, VehicleRequest request) {
        validateOwnership(request);
        vehicle.setGarageType(request.getGarageType());
        // COMPANY equipment never has an owner, regardless of what was sent.
        vehicle.setOwnerId(request.getGarageType() == GarageType.CUSTOMER ? request.getOwnerId() : null);
    }

    private void applyStatus(Vehicle vehicle, UUID branchId, String requestedStatus) {
        String status = requestedStatus == null || requestedStatus.isBlank() ? "Aktiv" : requestedStatus;
        configService.assertActiveValue(branchId, GarageConfigListType.STATUS, status);
        vehicle.setStatus(status);
    }

    /**
     * Null on the request means "copy the equipment type's configured default" — the brief's
     * "Konfiqurasiya bölməsində müəyyən edilə bilməli, konkret texnika səviyyəsində dəyişdirilə
     * bilməli". Falls back to (hours on, km off) when the type itself has no default set, which
     * matches how most of this brief's own examples behave (an excavator tracks hours, not km).
     */
    private void applyMeterUsage(Vehicle vehicle, UUID branchId, VehicleRequest request) {
        if (request.getUsesEngineHours() != null && request.getUsesKm() != null) {
            vehicle.setUsesEngineHours(request.getUsesEngineHours());
            vehicle.setUsesKm(request.getUsesKm());
            return;
        }
        GarageConfigValue type = configService.findEquipmentType(branchId, request.getVehicleType());
        boolean defaultHours = type != null && type.getDefaultUsesEngineHours() != null
                ? type.getDefaultUsesEngineHours() : true;
        boolean defaultKm = type != null && type.getDefaultUsesKm() != null ? type.getDefaultUsesKm() : false;
        vehicle.setUsesEngineHours(request.getUsesEngineHours() != null ? request.getUsesEngineHours() : defaultHours);
        vehicle.setUsesKm(request.getUsesKm() != null ? request.getUsesKm() : defaultKm);
    }

    /**
     * A brand-new vehicle's starting reading, set directly on the fast-read cache columns rather
     * than through Motosaat's {@code engine_hour_logs} — there is no prior reading for a rollover
     * trigger to reconcile against, so this is simply where the counter starts. Ignored on update:
     * once real readings may exist, only Motosaat's own recording flow may move the counter.
     */
    private void applyInitialMeterValue(Vehicle vehicle, VehicleRequest request) {
        if (request.getInitialMeterValue() == null) {
            return;
        }
        Instant now = Instant.now();
        if (Boolean.TRUE.equals(vehicle.getUsesEngineHours())) {
            vehicle.setCurrentEngineHours(request.getInitialMeterValue());
            vehicle.setLastEngineHoursAt(now);
        }
        if (Boolean.TRUE.equals(vehicle.getUsesKm())) {
            vehicle.setCurrentKm(request.getInitialMeterValue());
            vehicle.setLastKmAt(now);
        }
    }

    private List<String> nullToEmpty(List<String> values) {
        return values != null ? values : new ArrayList<>();
    }

    private String toJson(List<VehicleParameterItem> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(parameters);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** {@code x in ()} isn't portable — the repository query relies on this returning null. */
    private <T> List<T> emptyToNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values;
    }

    /** Open-ended fields grow their own pick list the first time a new value is typed. */
    private void registerOpenValues(UUID branchId, VehicleRequest request) {
        configService.ensureRegistered(branchId, GarageConfigListType.EQUIPMENT_TYPE, request.getVehicleType());
        configService.ensureRegistered(branchId, GarageConfigListType.BRAND, request.getMake());
        configService.ensureRegistered(branchId, GarageConfigListType.MODEL, request.getModel());
        if (request.getCurrentLocation() != null && !request.getCurrentLocation().isBlank()) {
            configService.ensureRegistered(branchId, GarageConfigListType.LOCATION, request.getCurrentLocation());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Vehicle loadVehicle(UUID id) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }
}
