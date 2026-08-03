package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryItemUnitBatchCreateRequest;
import com.ces.service.module.inventory.dto.InventoryItemUnitResponse;
import com.ces.service.module.inventory.dto.InventoryItemUnitUpdateRequest;
import com.ces.service.module.inventory.dto.MarkUnitFailedRequest;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryItemUnitRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serialized, individually warranty-tracked units of a serialized {@link InventoryItem} (e.g. one
 * battery out of a batch of 50, each with its own serial and warranty window).
 *
 * <p>Business rules:
 * <ul>
 *   <li>Units can only be registered against items with {@code isSerialized = true}
 *       ({@link ErrorCode#ITEM_NOT_SERIALIZED}).</li>
 *   <li>Serial numbers are unique within the branch ({@link ErrorCode#DUPLICATE_SERIAL_NUMBER}).</li>
 * </ul>
 */
@Service
@Transactional
public class InventoryItemUnitService {

    private final InventoryItemUnitRepository unitRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final InventoryAuditLogger auditLogger;

    public InventoryItemUnitService(
            InventoryItemUnitRepository unitRepository,
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            InventoryAuditLogger auditLogger) {
        this.unitRepository = unitRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemUnitResponse> listByItem(UUID itemId) {
        InventoryItem item = loadItem(itemId);
        return unitRepository.findByItemIdAndDeletedAtIsNullOrderByCreatedAtDesc(item.getId()).stream()
                .map(u -> InventoryItemUnitResponse.from(u, item.getName(), item.getSku()))
                .collect(Collectors.toList());
    }

    /** Warranty / general search across all units in the branch. */
    @Transactional(readOnly = true)
    public Page<InventoryItemUnitResponse> search(
            UUID itemId, InventoryUnitStatus status, String search, Pageable pageable) {
        UUID branchId = BranchContext.get();
        String searchPattern = toLikePattern(search);
        Page<InventoryItemUnit> page = status == null
                ? unitRepository.searchAllStatuses(branchId, itemId, searchPattern, pageable)
                : unitRepository.searchByStatus(branchId, itemId, status, searchPattern, pageable);
        List<UUID> itemIds = page.getContent().stream().map(InventoryItemUnit::getItemId).distinct().toList();
        var itemsById = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i));
        return page.map(u -> {
            InventoryItem item = itemsById.get(u.getItemId());
            return InventoryItemUnitResponse.from(u, item == null ? null : item.getName(), item == null ? null : item.getSku());
        });
    }

    @Transactional(readOnly = true)
    public InventoryItemUnitResponse get(UUID id) {
        InventoryItemUnit unit = loadUnit(id);
        InventoryItem item = itemRepository.findById(unit.getItemId()).orElse(null);
        return InventoryItemUnitResponse.from(unit, item == null ? null : item.getName(), item == null ? null : item.getSku());
    }

    public List<InventoryItemUnitResponse> createBatch(UUID itemId, InventoryItemUnitBatchCreateRequest request) {
        UUID branchId = BranchContext.get();
        InventoryItem item = loadItem(itemId);
        if (!Boolean.TRUE.equals(item.getIsSerialized())) {
            throw new BusinessException(ErrorCode.ITEM_NOT_SERIALIZED);
        }

        UUID nodeId = request.getNodeId() != null ? request.getNodeId() : item.getNodeId();
        InventoryNode node = nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(nodeId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory node not found: " + nodeId));

        LocalDate purchaseDate = request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDate.now();
        LocalDate warrantyStart = request.getWarrantyStartDate() != null ? request.getWarrantyStartDate() : purchaseDate;
        // The item's warrantyMonths is a default for its units: registering a batch shouldn't
        // require re-typing the same warranty length every time. An explicit end date still wins.
        LocalDate warrantyEnd = request.getWarrantyEndDate() != null
                ? request.getWarrantyEndDate()
                : WarrantyClock.endDateFrom(warrantyStart, item.getWarrantyMonths());

        // Reject duplicates within the submitted batch itself, then against existing data.
        Set<String> seen = new HashSet<>();
        for (String serial : request.getSerialNumbers()) {
            if (serial == null || serial.isBlank() || !seen.add(serial.trim())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NUMBER);
            }
        }
        for (String serial : seen) {
            if (unitRepository.existsByBranchIdAndSerialNumberAndDeletedAtIsNull(branchId, serial)) {
                throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NUMBER);
            }
        }

        List<InventoryItemUnit> units = new ArrayList<>();
        for (String serial : seen) {
            InventoryItemUnit unit = InventoryItemUnit.builder()
                    .itemId(item.getId())
                    .nodeId(node.getId())
                    .serialNumber(serial)
                    .status(InventoryUnitStatus.IN_STOCK)
                    .purchaseDate(purchaseDate)
                    .warrantyStartDate(warrantyStart)
                    .warrantyEndDate(warrantyEnd)
                    .notes(request.getNotes())
                    .build();
            unit.setBranchId(branchId);
            unit.setQrCode(UUID.randomUUID().toString());
            units.add(unit);
        }

        List<InventoryItemUnitResponse> saved = unitRepository.saveAll(units).stream()
                .map(u -> InventoryItemUnitResponse.from(u, item.getName(), item.getSku()))
                .collect(Collectors.toList());
        auditLogger.log(
                "CREATE",
                "INVENTORY_ITEM_UNIT",
                item.getId(),
                null,
                Map.of("serialNumbers", seen, "count", seen.size()));
        return saved;
    }

    public InventoryItemUnitResponse update(UUID id, InventoryItemUnitUpdateRequest request) {
        InventoryItemUnit unit = loadUnit(id);

        if (request.getNodeId() != null && !request.getNodeId().equals(unit.getNodeId())) {
            InventoryNode node = nodeRepository
                    .findByIdAndBranchIdAndDeletedAtIsNull(request.getNodeId(), unit.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory node not found: " + request.getNodeId()));
            unit.setNodeId(node.getId());
        }
        if (request.getStatus() != null) {
            unit.setStatus(request.getStatus());
        }
        if (request.getWarrantyStartDate() != null) {
            unit.setWarrantyStartDate(request.getWarrantyStartDate());
        }
        if (request.getWarrantyEndDate() != null) {
            unit.setWarrantyEndDate(request.getWarrantyEndDate());
        }
        if (request.getNotes() != null) {
            unit.setNotes(request.getNotes());
        }

        InventoryItem item = itemRepository.findById(unit.getItemId()).orElse(null);
        return InventoryItemUnitResponse.from(unit, item == null ? null : item.getName(), item == null ? null : item.getSku());
    }

    /** Records a unit failing — in or out of warranty, the search/status makes that visible. */
    public InventoryItemUnitResponse markFailed(UUID id, MarkUnitFailedRequest request) {
        InventoryItemUnit unit = loadUnit(id);
        InventoryUnitStatus before = unit.getStatus();
        unit.setStatus(InventoryUnitStatus.FAILED);
        unit.setFailedAt(Instant.now());
        unit.setFailureNotes(request.getFailureNotes());

        boolean withinWarranty = unit.getWarrantyEndDate() != null && !unit.getWarrantyEndDate().isBefore(LocalDate.now());
        auditLogger.log(
                "BUSINESS",
                "INVENTORY_ITEM_UNIT",
                unit.getId(),
                Map.of("status", before),
                Map.of(
                        "status", InventoryUnitStatus.FAILED,
                        "withinWarranty", withinWarranty,
                        "failureNotes", request.getFailureNotes() == null ? "" : request.getFailureNotes()));

        InventoryItem item = itemRepository.findById(unit.getItemId()).orElse(null);
        return InventoryItemUnitResponse.from(unit, item == null ? null : item.getName(), item == null ? null : item.getSku());
    }

    public void delete(UUID id) {
        InventoryItemUnit unit = loadUnit(id);
        unit.setDeletedAt(Instant.now());
        unitRepository.save(unit);
        auditLogger.log("DELETE", "INVENTORY_ITEM_UNIT", unit.getId(), null, null);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private InventoryItem loadItem(UUID id) {
        UUID branchId = BranchContext.get();
        return itemRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    private InventoryItemUnit loadUnit(UUID id) {
        UUID branchId = BranchContext.get();
        return unitRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item unit not found: " + id));
    }

    /** See {@code InventoryItemUnitRepository} javadoc for why this is built in Java. */
    private String toLikePattern(String search) {
        return (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";
    }
}
