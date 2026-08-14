package com.ces.service.module.enginehours.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.enginehours.dto.MeterReadingRequest;
import com.ces.service.module.enginehours.dto.MeterReadingResponse;
import com.ces.service.module.enginehours.dto.MeterRolloverRequest;
import com.ces.service.module.enginehours.entity.MeterReading;
import com.ces.service.module.enginehours.enums.MeterType;
import com.ces.service.module.enginehours.repository.MeterReadingRepository;
import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.VehicleRepository;
import com.ces.service.module.garage.service.GarageConfigService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motosaat/KM history (SRS M08). A reading is append-only: {@link #record} refuses a value below
 * the vehicle's current one outright, and the only way past that is a deliberate
 * {@link #applyRollover rollover} with a reason, reviewed through the approval queue — the
 * brief's "Motosaat redaktəsi/reset Təsdiqləmələr növbəsindən keçməli".
 */
@Service
@Transactional
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageConfigService configService;
    private final MotosaatAuditLogger auditLogger;

    public MeterReadingService(
            MeterReadingRepository meterReadingRepository,
            VehicleRepository vehicleRepository,
            GarageConfigService configService,
            MotosaatAuditLogger auditLogger) {
        this.meterReadingRepository = meterReadingRepository;
        this.vehicleRepository = vehicleRepository;
        this.configService = configService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public Page<MeterReadingResponse> history(UUID vehicleId, Pageable pageable) {
        loadVehicle(vehicleId);
        return meterReadingRepository
                .findByVehicleIdAndDeletedAtIsNull(vehicleId, pageable)
                .map(MeterReadingResponse::from);
    }

    public MeterReadingResponse record(UUID vehicleId, MeterReadingRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        assertMeterEnabled(vehicle, request.getMeterType());
        BigDecimal previous = currentValue(vehicle, request.getMeterType());
        if (request.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (previous != null && request.getValue().compareTo(previous) < 0) {
            throw new BusinessException(ErrorCode.ENGINE_HOURS_DECREASING);
        }
        String source = request.getSource() == null || request.getSource().isBlank() ? "Manual" : request.getSource();
        configService.assertActiveValue(vehicle.getBranchId(), GarageConfigListType.METER_SOURCE, source);

        MeterReading reading = MeterReading.builder()
                .vehicleId(vehicleId)
                .meterType(request.getMeterType())
                .value(request.getValue())
                .previousValue(previous)
                .source(source)
                .isRollover(false)
                .recordedAt(request.getRecordedAt() == null ? LocalDate.now() : request.getRecordedAt())
                .notes(request.getNotes())
                .build();
        return saveAndLog(vehicle.getBranchId(), reading);
    }

    /**
     * Records a reading on behalf of another module (e.g. a maintenance completion) in the same
     * transaction, skipping the decrease guard's exception surface — callers already decided this
     * value is legitimate. Still refuses an actual decrease; a completion reporting a lower meter
     * value than the vehicle already has is a data-entry mistake, not a rollover.
     */
    public MeterReadingResponse recordFromSource(
            UUID vehicleId, MeterType meterType, BigDecimal value, LocalDate recordedAt, String source, UUID sourceRefId) {
        Vehicle vehicle = loadVehicle(vehicleId);
        assertMeterEnabled(vehicle, meterType);
        BigDecimal previous = currentValue(vehicle, meterType);
        if (previous != null && value.compareTo(previous) < 0) {
            throw new BusinessException(ErrorCode.ENGINE_HOURS_DECREASING);
        }
        configService.assertActiveValue(vehicle.getBranchId(), GarageConfigListType.METER_SOURCE, source);

        MeterReading reading = MeterReading.builder()
                .vehicleId(vehicleId)
                .meterType(meterType)
                .value(value)
                .previousValue(previous)
                .source(source)
                .isRollover(false)
                .sourceRefId(sourceRefId)
                .recordedAt(recordedAt == null ? LocalDate.now() : recordedAt)
                .build();
        return saveAndLog(vehicle.getBranchId(), reading);
    }

    /** Validates before the controller parks the rollover for approval. */
    @Transactional(readOnly = true)
    public void assertRolloverValid(UUID vehicleId, MeterRolloverRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        assertMeterEnabled(vehicle, request.getMeterType());
        if (request.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.METER_ROLLOVER_REASON_REQUIRED);
        }
    }

    /** Replays an approved rollover. Called only by {@link MeterReadingApprovalExecutor}. */
    public MeterReadingResponse applyRollover(UUID vehicleId, MeterRolloverRequest request) {
        Vehicle vehicle = loadVehicle(vehicleId);
        BigDecimal previous = currentValue(vehicle, request.getMeterType());

        MeterReading reading = MeterReading.builder()
                .vehicleId(vehicleId)
                .meterType(request.getMeterType())
                .value(request.getValue())
                .previousValue(previous)
                .source("Manual")
                .isRollover(true)
                .rolloverReason(request.getReason())
                .recordedAt(request.getRecordedAt() == null ? LocalDate.now() : request.getRecordedAt())
                .notes(request.getNotes())
                .build();
        MeterReadingResponse response = saveAndLog(vehicle.getBranchId(), reading);
        auditLogger.log("BUSINESS", "METER_ROLLOVER", reading.getId(), previous, response);
        return response;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private MeterReadingResponse saveAndLog(UUID branchId, MeterReading reading) {
        reading.setBranchId(branchId);
        // flush, not save: `delta` is DB-generated (value - previous_value) and only visible once
        // the INSERT actually reaches the database — the same @Generated/write-behind trap
        // Vehicle.code hit, documented in backend/docs/qaraj-motosaat-plani.md.
        MeterReading saved = meterReadingRepository.saveAndFlush(reading);
        MeterReadingResponse response = MeterReadingResponse.from(saved);
        auditLogger.log("CREATE", "METER_READING", saved.getId(), null, response);
        return response;
    }

    private void assertMeterEnabled(Vehicle vehicle, MeterType meterType) {
        boolean enabled = meterType == MeterType.ENGINE_HOURS
                ? Boolean.TRUE.equals(vehicle.getUsesEngineHours())
                : Boolean.TRUE.equals(vehicle.getUsesKm());
        if (!enabled) {
            throw new BusinessException(ErrorCode.METER_TYPE_NOT_ENABLED);
        }
    }

    private BigDecimal currentValue(Vehicle vehicle, MeterType meterType) {
        return meterType == MeterType.ENGINE_HOURS ? vehicle.getCurrentEngineHours() : vehicle.getCurrentKm();
    }

    private Vehicle loadVehicle(UUID vehicleId) {
        return vehicleRepository.findByIdAndBranchIdAndDeletedAtIsNull(vehicleId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }
}
