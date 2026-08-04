package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.WarrantyClaimDecisionRequest;
import com.ces.service.module.inventory.dto.WarrantyClaimRequest;
import com.ces.service.module.inventory.dto.WarrantyClaimResponse;
import com.ces.service.module.inventory.dto.WarrantyExtendRequest;
import com.ces.service.module.inventory.dto.WarrantyExtensionResponse;
import com.ces.service.module.inventory.dto.WarrantyRecordResponse;
import com.ces.service.module.inventory.dto.WarrantyRecordSearchCriteria;
import com.ces.service.module.inventory.dto.WarrantySummaryResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.entity.WarrantyClaim;
import com.ces.service.module.inventory.entity.WarrantyExtension;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryItemUnitRepository;
import com.ces.service.module.inventory.repository.WarrantyClaimRepository;
import com.ces.service.module.inventory.repository.WarrantyExtensionRepository;
import com.ces.service.module.inventory.repository.WarrantyRecordRepository;
import com.ces.service.module.inventory.repository.WarrantyRecordRow;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final WarrantyRecordRepository recordRepository;
    private final WarrantyClaimRepository claimRepository;
    private final InventoryAuditLogger auditLogger;

    public WarrantyService(
            InventoryItemRepository itemRepository,
            InventoryItemUnitRepository unitRepository,
            WarrantyExtensionRepository extensionRepository,
            WarrantyRecordRepository recordRepository,
            WarrantyClaimRepository claimRepository,
            InventoryAuditLogger auditLogger) {
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
        this.extensionRepository = extensionRepository;
        this.recordRepository = recordRepository;
        this.claimRepository = claimRepository;
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
                unitRepository.countUnitsWithWarrantyExpired(branchId, today),
                claimRepository.countOpen(branchId));
    }

    // ── Unified search ───────────────────────────────────────────────────

    /**
     * Every warranty in the branch — serialized units and non-serialized products in one list.
     *
     * <p>Each row is stamped with its most recent claim so the answer to "has anyone chased this?"
     * is visible without opening it. The claims are fetched in one query for the whole page rather
     * than per row.
     */
    @Transactional(readOnly = true)
    public Page<WarrantyRecordResponse> search(WarrantyRecordSearchCriteria criteria, Pageable pageable) {
        UUID branchId = BranchContext.get();
        LocalDate today = LocalDate.now();
        LocalDate soonDate = today.plusDays(WarrantyClock.EXPIRING_SOON_DAYS);

        LocalDate endFrom = criteria.getEndFrom();
        LocalDate endTo = criteria.getEndTo();
        if (endFrom == null && endTo == null && criteria.getWithinDays() != null) {
            endFrom = today;
            endTo = today.plusDays(criteria.getWithinDays());
        }

        Page<WarrantyRecordRow> page = recordRepository.search(
                branchId,
                toLikePattern(criteria.getSearch()),
                criteria.getRecordType() == null ? null : criteria.getRecordType().name(),
                criteria.getWarrantyStatus() == null ? null : criteria.getWarrantyStatus().name(),
                criteria.getUnitStatus() == null ? null : criteria.getUnitStatus().name(),
                blankToNull(criteria.getSupplier()),
                endFrom,
                endTo,
                today,
                soonDate,
                pageable);

        Map<UUID, WarrantyClaimResponse> latestClaims = latestClaimsFor(
                branchId, page.getContent().stream().map(WarrantyRecordRow::getRecordId).toList());
        return page.map(row -> WarrantyRecordResponse.from(row, today, latestClaims.get(row.getRecordId())));
    }

    /** Suppliers actually present on products — the filter dropdown, not a free-text guess. */
    @Transactional(readOnly = true)
    public List<String> suppliers() {
        return recordRepository.findDistinctSuppliers(BranchContext.get());
    }

    // ── Claims ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<WarrantyClaimResponse> listClaims(WarrantyClaimStatus status, String search, Pageable pageable) {
        UUID branchId = BranchContext.get();
        String pattern = toLikePattern(search);
        Page<WarrantyClaim> page = status == null
                ? claimRepository.searchAllStatuses(branchId, pattern, pageable)
                : claimRepository.searchByStatus(branchId, status, pattern, pageable);
        return page.map(WarrantyClaimResponse::from);
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> claimsForTarget(WarrantyTargetType targetType, UUID targetId) {
        return claimRepository
                .findByBranchIdAndTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        BranchContext.get(), targetType, targetId)
                .stream()
                .map(WarrantyClaimResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarrantyClaimResponse getClaim(UUID id) {
        return WarrantyClaimResponse.from(loadClaim(id));
    }

    /**
     * Files a claim. The label, product and supplier are snapshotted here: the claim is a record of
     * what was sent on a given day, and renaming the product later must not rewrite history.
     */
    public WarrantyClaimResponse createClaim(WarrantyClaimRequest request) {
        UUID branchId = BranchContext.get();

        String label;
        UUID itemId;
        String defaultSupplier;
        if (request.getTargetType() == WarrantyTargetType.INVENTORY_ITEM_UNIT) {
            InventoryItemUnit unit = unitRepository
                    .findByIdAndBranchIdAndDeletedAtIsNull(request.getTargetId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item unit not found: " + request.getTargetId()));
            InventoryItem item = itemRepository.findById(unit.getItemId()).orElse(null);
            label = unit.getSerialNumber();
            itemId = unit.getItemId();
            defaultSupplier = item == null ? null : item.getSupplier();
        } else {
            InventoryItem item = itemRepository
                    .findByIdAndBranchIdAndDeletedAtIsNull(request.getTargetId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found: " + request.getTargetId()));
            label = item.getName();
            itemId = item.getId();
            defaultSupplier = item.getSupplier();
        }

        WarrantyClaim claim = WarrantyClaim.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetLabel(label)
                .itemId(itemId)
                .supplier(blankToNull(request.getSupplier()) != null ? request.getSupplier().trim() : defaultSupplier)
                .claimNumber(blankToNull(request.getClaimNumber()))
                .status(WarrantyClaimStatus.SUBMITTED)
                .description(request.getDescription())
                .submittedAt(request.getSubmittedAt() != null ? request.getSubmittedAt() : LocalDate.now())
                .build();
        claim.setBranchId(branchId);

        WarrantyClaim saved = claimRepository.save(claim);
        auditLogger.log(
                "CREATE",
                "WARRANTY_CLAIM",
                saved.getId(),
                null,
                Map.of(
                        "targetType", String.valueOf(saved.getTargetType()),
                        "targetLabel", String.valueOf(saved.getTargetLabel()),
                        "supplier", String.valueOf(saved.getSupplier())));
        return WarrantyClaimResponse.from(saved);
    }

    /**
     * Records the supplier's answer — the point where the module finally says who pays. Going back
     * to SUBMITTED clears the decision rather than leaving a stale date next to "no answer yet".
     */
    public WarrantyClaimResponse decideClaim(UUID id, WarrantyClaimDecisionRequest request) {
        WarrantyClaim claim = loadClaim(id);
        WarrantyClaimStatus before = claim.getStatus();

        claim.setStatus(request.getStatus());
        claim.setResolution(request.getResolution());
        if (request.getDecisionNotes() != null) {
            claim.setDecisionNotes(request.getDecisionNotes());
        }
        if (request.getStatus() == WarrantyClaimStatus.SUBMITTED) {
            claim.setDecidedAt(null);
        } else {
            claim.setDecidedAt(request.getDecidedAt() != null ? request.getDecidedAt() : LocalDate.now());
        }

        auditLogger.log(
                "BUSINESS",
                "WARRANTY_CLAIM",
                claim.getId(),
                Map.of("status", String.valueOf(before)),
                Map.of(
                        "status", String.valueOf(claim.getStatus()),
                        "resolution", String.valueOf(claim.getResolution())));
        return WarrantyClaimResponse.from(claim);
    }

    public void deleteClaim(UUID id) {
        WarrantyClaim claim = loadClaim(id);
        claim.setDeletedAt(java.time.Instant.now());
        claimRepository.save(claim);
        auditLogger.log("DELETE", "WARRANTY_CLAIM", claim.getId(), null, null);
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

    private WarrantyClaim loadClaim(UUID id) {
        return claimRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + id));
    }

    /**
     * Newest claim per target, from a single query. The repository returns them newest-first, so
     * keeping only the first occurrence of each target id gives the latest without sorting again.
     */
    private Map<UUID, WarrantyClaimResponse> latestClaimsFor(UUID branchId, List<UUID> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, WarrantyClaimResponse> byTarget = new HashMap<>();
        for (WarrantyClaim claim : claimRepository.findByTargets(branchId, targetIds)) {
            byTarget.putIfAbsent(claim.getTargetId(), WarrantyClaimResponse.from(claim));
        }
        return byTarget;
    }

    /** See {@code InventoryItemUnitRepository} javadoc for why this is built in Java. */
    private String toLikePattern(String search) {
        return (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

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
