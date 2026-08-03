package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import com.ces.service.module.inventory.dto.WarrantyExtensionResponse;
import com.ces.service.module.inventory.dto.WarrantySummaryResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.entity.WarrantyExtension;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryItemUnitRepository;
import com.ces.service.module.inventory.repository.WarrantyExtensionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Warranty windows and their extensions.
 *
 * <p>Extending is deliberately not a plain field edit: it moves a date that decides who pays for a
 * repair, so it goes through the approval queue like any other destructive change and leaves a
 * {@link WarrantyExtension} row behind once applied. That row is the audit answer to "who gave
 * this extra year, and why".
 */
@Service
@Transactional
public class WarrantyService {

    private final InventoryItemRepository itemRepository;
    private final InventoryItemUnitRepository unitRepository;
    private final WarrantyExtensionRepository extensionRepository;
    private final InventoryAuditLogger auditLogger;

    public WarrantyService(
            InventoryItemRepository itemRepository,
            InventoryItemUnitRepository unitRepository,
            WarrantyExtensionRepository extensionRepository,
            InventoryAuditLogger auditLogger) {
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
        this.extensionRepository = extensionRepository;
        this.auditLogger = auditLogger;
    }

    /** Counts behind the sidebar badge and dashboard card. */
    @Transactional(readOnly = true)
    public WarrantySummaryResponse summary() {
        UUID branchId = BranchContext.get();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(WarrantyClock.EXPIRING_SOON_DAYS);
        return WarrantySummaryResponse.of(
                itemRepository.countItemsWithWarrantyEndBetween(branchId, today, horizon),
                unitRepository.countUnitsWithWarrantyEndBetween(branchId, today, horizon),
                itemRepository.countItemsWithWarrantyExpired(branchId, today),
                unitRepository.countUnitsWithWarrantyExpired(branchId, today));
    }

    @Transactional(readOnly = true)
    public List<WarrantyExtensionResponse> history(WarrantyTargetType targetType, UUID targetId) {
        return extensionRepository
                .findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(targetType, targetId)
                .stream()
                .map(WarrantyExtensionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Applies an approved extension to a product. Called by the approval executor, never straight
     * from a controller — the endpoint only parks the request.
     */
    public WarrantyExtensionResponse extendItem(UUID itemId, WarrantyExtendRequest request) {
        InventoryItem item = itemRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(itemId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));

        // A serialized product has no warranty window of its own — extending it would silently do
        // nothing while the units it's meant to cover stay unchanged.
        if (Boolean.TRUE.equals(item.getIsSerialized())) {
            throw new BusinessException(ErrorCode.ITEM_IS_SERIALIZED);
        }

        LocalDate previous = item.getWarrantyEndDate();
        LocalDate next = resolveNewEndDate(previous, request);
        item.setWarrantyEndDate(next);
        if (item.getWarrantyStartDate() == null) {
            item.setWarrantyStartDate(LocalDate.now());
        }

        WarrantyExtension extension = record(
                WarrantyTargetType.INVENTORY_ITEM, item.getId(), item.getName(), previous, next, request);
        auditLogger.log(
                "BUSINESS",
                "INVENTORY_ITEM",
                item.getId(),
                Map.of("warrantyEndDate", String.valueOf(previous)),
                Map.of("warrantyEndDate", String.valueOf(next)));
        return WarrantyExtensionResponse.from(extension);
    }

    /** Applies an approved extension to one serialized unit. */
    public WarrantyExtensionResponse extendUnit(UUID unitId, WarrantyExtendRequest request) {
        InventoryItemUnit unit = unitRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(unitId, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item unit not found: " + unitId));

        LocalDate previous = unit.getWarrantyEndDate();
        LocalDate next = resolveNewEndDate(previous, request);
        unit.setWarrantyEndDate(next);
        if (unit.getWarrantyStartDate() == null) {
            unit.setWarrantyStartDate(LocalDate.now());
        }

        WarrantyExtension extension = record(
                WarrantyTargetType.INVENTORY_ITEM_UNIT,
                unit.getId(),
                unit.getSerialNumber(),
                previous,
                next,
                request);
        auditLogger.log(
                "BUSINESS",
                "INVENTORY_ITEM_UNIT",
                unit.getId(),
                Map.of("warrantyEndDate", String.valueOf(previous)),
                Map.of("warrantyEndDate", String.valueOf(next)));
        return WarrantyExtensionResponse.from(extension);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Months are added to whichever is later — the current end date or today. Extending an already
     * expired warranty by 6 months should give 6 months of cover from now, not 6 months from a date
     * that has already passed (which could still leave it expired).
     */
    private LocalDate resolveNewEndDate(LocalDate previous, WarrantyExtendRequest request) {
        if (request.getNewEndDate() != null) {
            return request.getNewEndDate();
        }
        if (request.getMonths() == null || request.getMonths() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        LocalDate today = LocalDate.now();
        LocalDate base = (previous == null || previous.isBefore(today)) ? today : previous;
        return base.plusMonths(request.getMonths());
    }

    private WarrantyExtension record(
            WarrantyTargetType targetType,
            UUID targetId,
            String label,
            LocalDate previous,
            LocalDate next,
            WarrantyExtendRequest request) {
        WarrantyExtension extension = WarrantyExtension.builder()
                .targetType(targetType)
                .targetId(targetId)
                .targetLabel(label)
                .previousEndDate(previous)
                .newEndDate(next)
                .monthsAdded(request.getNewEndDate() != null ? null : request.getMonths())
                .reason(request.getReason())
                .build();
        extension.setBranchId(BranchContext.get());
        return extensionRepository.save(extension);
    }
}
