package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.GarageConfigValueRequest;
import com.ces.service.module.garage.dto.GarageConfigValueResponse;
import com.ces.service.module.garage.entity.GarageConfigValue;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.GarageConfigValueRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every manageable dropdown list Qaraj uses — one table, one service. See
 * {@link GarageConfigListType} for why this replaces Inventory's full category/field system here:
 * Qaraj never needs a per-type custom field schema, only dropdown values.
 *
 * <p>Two distinct ways a value stops being offered, and they are not interchangeable:
 * <ul>
 *   <li><b>Deactivate</b> ({@code isActive = false}, via {@link #update}) — reversible, the row
 *       stays so historical records that used it keep meaning something.</li>
 *   <li><b>Delete</b> (soft delete via {@link #delete}) — for values created by mistake.
 *       System-seeded rows ({@code isSystem}) refuse this: other code names them by exact string
 *       (a status column, a meter source), so the string has to keep existing even if hidden.</li>
 * </ul>
 */
@Service
@Transactional
public class GarageConfigService {

    private final GarageConfigValueRepository repository;
    private final GarageAuditLogger auditLogger;

    public GarageConfigService(GarageConfigValueRepository repository, GarageAuditLogger auditLogger) {
        this.repository = repository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<GarageConfigValueResponse> list(GarageConfigListType listType) {
        UUID branchId = BranchContext.get();
        List<GarageConfigValue> values = listType == null
                ? repository.findByBranchIdAndDeletedAtIsNullOrderByListTypeAscSortOrderAsc(branchId)
                : repository.findByBranchIdAndListTypeAndDeletedAtIsNullOrderBySortOrderAsc(branchId, listType);
        return values.stream().map(GarageConfigValueResponse::from).collect(Collectors.toList());
    }

    public GarageConfigValueResponse create(GarageConfigValueRequest request) {
        UUID branchId = BranchContext.get();
        if (repository.existsByBranchIdAndListTypeAndValueAndDeletedAtIsNull(
                branchId, request.getListType(), request.getValue())) {
            throw new BusinessException(ErrorCode.GARAGE_CONFIG_DUPLICATE);
        }
        GarageConfigValue value = GarageConfigValue.builder()
                .listType(request.getListType())
                .value(request.getValue())
                .isRequired(request.getIsRequired() != null && request.getIsRequired())
                .defaultUsesEngineHours(request.getDefaultUsesEngineHours())
                .defaultUsesKm(request.getDefaultUsesKm())
                .isSystem(false)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .sortOrder(request.getSortOrder() == null ? nextSortOrder(branchId, request.getListType()) : request.getSortOrder())
                .build();
        value.setBranchId(branchId);
        GarageConfigValue saved = repository.save(value);
        auditLogger.log("CREATE", "GARAGE_CONFIG_VALUE", saved.getId(), null, GarageConfigValueResponse.from(saved));
        return GarageConfigValueResponse.from(saved);
    }

    public GarageConfigValueResponse update(UUID id, GarageConfigValueRequest request) {
        GarageConfigValue value = loadValue(id);
        GarageConfigValueResponse before = GarageConfigValueResponse.from(value);

        if (!value.getValue().equals(request.getValue())
                && repository.existsByBranchIdAndListTypeAndValueAndDeletedAtIsNullAndIdNot(
                        value.getBranchId(), value.getListType(), request.getValue(), id)) {
            throw new BusinessException(ErrorCode.GARAGE_CONFIG_DUPLICATE);
        }

        value.setValue(request.getValue());
        if (request.getIsRequired() != null) {
            value.setIsRequired(request.getIsRequired());
        }
        if (request.getDefaultUsesEngineHours() != null) {
            value.setDefaultUsesEngineHours(request.getDefaultUsesEngineHours());
        }
        if (request.getDefaultUsesKm() != null) {
            value.setDefaultUsesKm(request.getDefaultUsesKm());
        }
        if (request.getIsActive() != null) {
            value.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            value.setSortOrder(request.getSortOrder());
        }
        GarageConfigValueResponse after = GarageConfigValueResponse.from(value);
        auditLogger.log("UPDATE", "GARAGE_CONFIG_VALUE", id, before, after);
        return after;
    }

    public void delete(UUID id) {
        GarageConfigValue value = loadValue(id);
        if (Boolean.TRUE.equals(value.getIsSystem())) {
            throw new BusinessException(ErrorCode.GARAGE_CONFIG_SYSTEM_PROTECTED);
        }
        value.setDeletedAt(Instant.now());
        auditLogger.log("DELETE", "GARAGE_CONFIG_VALUE", id, GarageConfigValueResponse.from(value), null);
    }

    /**
     * Registers a value the first time it is typed on an open-ended field (make, model,
     * equipment type, location, ...), so it becomes a reusable choice from then on — the brief's
     * "yeni marka gələcək qeydiyyatlarda seçim kimi görünsün". A no-op if the value already
     * exists, active or not.
     */
    public void ensureRegistered(UUID branchId, GarageConfigListType listType, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (repository.existsByBranchIdAndListTypeAndValueAndDeletedAtIsNull(branchId, listType, value)) {
            return;
        }
        GarageConfigValue row = GarageConfigValue.builder()
                .listType(listType)
                .value(value)
                .isRequired(false)
                .isSystem(false)
                .isActive(true)
                .sortOrder(nextSortOrder(branchId, listType))
                .build();
        row.setBranchId(branchId);
        repository.save(row);
    }

    /**
     * Closed-set validation for fields the admin curates rather than the field auto-growing
     * (currently just STATUS) — unlike {@link #ensureRegistered}, an unknown value is rejected,
     * not silently added, because the value drives real business logic elsewhere.
     */
    @Transactional(readOnly = true)
    public void assertActiveValue(UUID branchId, GarageConfigListType listType, String value) {
        boolean valid = repository.findByBranchIdAndListTypeAndValueAndDeletedAtIsNull(branchId, listType, value)
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .isPresent();
        if (!valid) {
            throw new BusinessException(ErrorCode.GARAGE_CONFIG_VALUE_INVALID);
        }
    }

    /** The equipment type's own defaults, used to seed a new vehicle's meter-usage flags. */
    @Transactional(readOnly = true)
    public GarageConfigValue findEquipmentType(UUID branchId, String value) {
        return repository
                .findByBranchIdAndListTypeAndValueAndDeletedAtIsNull(branchId, GarageConfigListType.EQUIPMENT_TYPE, value)
                .orElse(null);
    }

    private int nextSortOrder(UUID branchId, GarageConfigListType listType) {
        return repository.findByBranchIdAndListTypeAndDeletedAtIsNullOrderBySortOrderAsc(branchId, listType).stream()
                .mapToInt(GarageConfigValue::getSortOrder)
                .max()
                .orElse(0) + 1;
    }

    private GarageConfigValue loadValue(UUID id) {
        return repository.findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Garage config value not found: " + id));
    }
}
