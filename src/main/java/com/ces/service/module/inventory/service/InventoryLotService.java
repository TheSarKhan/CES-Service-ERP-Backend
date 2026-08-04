package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryLotRequest;
import com.ces.service.module.inventory.dto.InventoryLotResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryLot;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryLotRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batches with their own expiry dates, and the FEFO rule that keeps the oldest moving.
 *
 * <p>FEFO is a suggestion, not a constraint. The system orders batches by how soon they expire and
 * puts the first one in front of the person picking, but a real warehouse has reasons to take
 * another — a customer returning a specific batch, a damaged pallet at the front. Refusing those
 * would only teach people to record the wrong batch.
 */
@Service
@Transactional
public class InventoryLotService {

    /** Same horizon the warranty band uses, unless the product sets its own. */
    private static final int DEFAULT_WARNING_DAYS = WarrantyClock.EXPIRING_SOON_DAYS;

    private final InventoryLotRepository lotRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final StockLedger stockLedger;
    private final InventoryAuditLogger auditLogger;

    public InventoryLotService(
            InventoryLotRepository lotRepository,
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            StockLedger stockLedger,
            InventoryAuditLogger auditLogger) {
        this.lotRepository = lotRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.stockLedger = stockLedger;
        this.auditLogger = auditLogger;
    }

    // ── reads ────────────────────────────────────────────────────────────

    /** Every batch of a product, in FEFO order within each folder. */
    @Transactional(readOnly = true)
    public List<InventoryLotResponse> listForItem(UUID itemId) {
        InventoryItem item = loadItem(itemId);
        LocalDate today = LocalDate.now();
        int warningDays = warningDaysOf(item);
        Map<UUID, String> nodeNames = nodeNames(
                lotRepository.findByItemIdAndDeletedAtIsNull(itemId).stream()
                        .map(InventoryLot::getNodeId)
                        .distinct()
                        .toList());

        return lotRepository.findByItemIdAndDeletedAtIsNull(itemId).stream()
                .sorted((a, b) -> {
                    if (a.getExpiryDate() == null && b.getExpiryDate() == null) return 0;
                    if (a.getExpiryDate() == null) return 1;
                    if (b.getExpiryDate() == null) return -1;
                    return a.getExpiryDate().compareTo(b.getExpiryDate());
                })
                .map(lot -> InventoryLotResponse.from(
                        lot, item.getName(), nodeNames.get(lot.getNodeId()), today, warningDays))
                .collect(Collectors.toList());
    }

    /** The batch FEFO would pick at a folder, or null when there is nothing to pick. */
    @Transactional(readOnly = true)
    public InventoryLotResponse suggestFor(UUID itemId, UUID nodeId) {
        InventoryItem item = loadItem(itemId);
        List<InventoryLot> lots = lotRepository.findFefoOrder(itemId, nodeId);
        if (lots.isEmpty()) {
            return null;
        }
        InventoryLot first = lots.get(0);
        return InventoryLotResponse.from(
                first, item.getName(), nodeName(first.getNodeId()), LocalDate.now(), warningDaysOf(item));
    }

    /** Batches running out of time across the branch. */
    @Transactional(readOnly = true)
    public Page<InventoryLotResponse> expiring(int withinDays, Pageable pageable) {
        UUID branchId = BranchContext.get();
        LocalDate today = LocalDate.now();
        Page<InventoryLot> page =
                lotRepository.findExpiring(branchId, today.plusDays(withinDays), pageable);

        Map<UUID, String> itemNames = itemRepository
                .findAllById(page.getContent().stream().map(InventoryLot::getItemId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(InventoryItem::getId, InventoryItem::getName));
        Map<UUID, String> nodeNames = nodeNames(
                page.getContent().stream().map(InventoryLot::getNodeId).distinct().toList());

        return page.map(lot -> InventoryLotResponse.from(
                lot,
                itemNames.get(lot.getItemId()),
                nodeNames.get(lot.getNodeId()),
                today,
                DEFAULT_WARNING_DAYS));
    }

    /** Counts behind the expiry band. */
    @Transactional(readOnly = true)
    public long[] expirySummary() {
        UUID branchId = BranchContext.get();
        LocalDate today = LocalDate.now();
        return new long[] {
            lotRepository.countExpiringSoon(branchId, today, today.plusDays(DEFAULT_WARNING_DAYS)),
            lotRepository.countExpired(branchId, today),
        };
    }

    // ── writes ───────────────────────────────────────────────────────────

    /**
     * Registers an arriving batch and brings its quantity into stock.
     *
     * <p>Re-registering an existing lot number at the same folder adds to it rather than creating a
     * second row — the same batch arriving in two deliveries is still one batch.
     */
    public InventoryLotResponse receive(UUID itemId, InventoryLotRequest request) {
        UUID branchId = BranchContext.get();
        InventoryItem item = loadItem(itemId);
        if (!Boolean.TRUE.equals(item.getIsLotTracked())) {
            throw new BusinessException(ErrorCode.LOT_NOT_TRACKED);
        }
        InventoryNode node = nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(request.getNodeId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory node not found: " + request.getNodeId()));

        InventoryLot lot = lotRepository
                .findByItemIdAndNodeIdAndLotNumberAndDeletedAtIsNull(
                        itemId, node.getId(), request.getLotNumber().trim())
                .orElseGet(() -> {
                    InventoryLot created = InventoryLot.builder()
                            .itemId(itemId)
                            .nodeId(node.getId())
                            .lotNumber(request.getLotNumber().trim())
                            .quantity(BigDecimal.ZERO)
                            .receivedDate(request.getReceivedDate() != null
                                    ? request.getReceivedDate()
                                    : LocalDate.now())
                            .build();
                    created.setBranchId(branchId);
                    return created;
                });

        lot.setQuantity(lot.getQuantity().add(request.getQuantity()));
        if (request.getExpiryDate() != null) {
            lot.setExpiryDate(request.getExpiryDate());
        }
        if (request.getNotes() != null) {
            lot.setNotes(request.getNotes());
        }
        InventoryLot saved = lotRepository.save(lot);

        // The batch is stock: it goes through the ledger like everything else, or the folder total
        // and the sum of its batches would drift apart.
        stockLedger.move(
                itemId,
                node.getId(),
                StockMovementType.IN,
                request.getQuantity(),
                "LOT",
                saved.getId(),
                "Partiya: " + saved.getLotNumber());

        auditLogger.log("CREATE", "INVENTORY_LOT", saved.getId(), null,
                Map.of("lotNumber", saved.getLotNumber(), "quantity", request.getQuantity()));
        return InventoryLotResponse.from(
                saved, item.getName(), node.getName(), LocalDate.now(), warningDaysOf(item));
    }

    /** Takes quantity out of a specific batch — the picking half of FEFO. */
    public InventoryLotResponse consume(UUID lotId, BigDecimal quantity, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        InventoryLot lot = loadLot(lotId);
        if (lot.getQuantity().compareTo(quantity) < 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        lot.setQuantity(lot.getQuantity().subtract(quantity));

        stockLedger.move(
                lot.getItemId(),
                lot.getNodeId(),
                StockMovementType.OUT,
                quantity.negate(),
                "LOT",
                lot.getId(),
                reason == null ? "Partiya: " + lot.getLotNumber() : reason);

        InventoryItem item = itemRepository.findById(lot.getItemId()).orElse(null);
        return InventoryLotResponse.from(
                lot,
                item == null ? null : item.getName(),
                nodeName(lot.getNodeId()),
                LocalDate.now(),
                item == null ? DEFAULT_WARNING_DAYS : warningDaysOf(item));
    }

    /** Writes off an expired batch: the stock leaves, the row stays as history. */
    public void writeOff(UUID lotId, String reason) {
        InventoryLot lot = loadLot(lotId);
        if (lot.getQuantity().signum() > 0) {
            stockLedger.move(
                    lot.getItemId(),
                    lot.getNodeId(),
                    StockMovementType.OUT,
                    lot.getQuantity().negate(),
                    "LOT",
                    lot.getId(),
                    reason == null ? "Partiya silindi: " + lot.getLotNumber() : reason);
            lot.setQuantity(BigDecimal.ZERO);
        }
        lot.setDeletedAt(Instant.now());
        lotRepository.save(lot);
        auditLogger.log("DELETE", "INVENTORY_LOT", lot.getId(), null,
                Map.of("lotNumber", lot.getLotNumber()));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private int warningDaysOf(InventoryItem item) {
        Integer configured = item.getExpiryWarningDays();
        return configured == null || configured <= 0 ? DEFAULT_WARNING_DAYS : configured;
    }

    private InventoryItem loadItem(UUID id) {
        return itemRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    private InventoryLot loadLot(UUID id) {
        return lotRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory lot not found: " + id));
    }

    private String nodeName(UUID nodeId) {
        return nodeRepository.findById(nodeId).map(InventoryNode::getName).orElse(null);
    }

    private Map<UUID, String> nodeNames(List<UUID> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Map.of();
        }
        return nodeRepository.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(InventoryNode::getId, InventoryNode::getName));
    }
}
