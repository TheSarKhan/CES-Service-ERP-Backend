package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.inventory.dto.TransferRequest;
import com.ces.service.module.inventory.dto.TransferResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.entity.InventoryTransfer;
import com.ces.service.module.inventory.entity.InventoryTransferLine;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.enums.TransferStatus;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import com.ces.service.module.inventory.repository.InventoryTransferLineRepository;
import com.ces.service.module.inventory.repository.InventoryTransferRepository;
import com.ces.service.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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
 * Stock moving between folders, in two steps.
 *
 * <p>Sending takes the quantity off the source immediately: it has physically left, and leaving it
 * on the shelf would let the same units be promised twice. It reaches the destination only when
 * somebody receives it. Both steps are ledger movements, so the trolley never becomes an
 * unexplained gap in a count.
 *
 * <p>There is no approval queue here — receiving *is* the second pair of eyes. Whether the receiver
 * must be a different person is a branch setting: necessary control in a large warehouse, a total
 * blocker in one with a single storekeeper.
 */
@Service
@Transactional
public class InventoryTransferService {

    private final InventoryTransferRepository transferRepository;
    private final InventoryTransferLineRepository lineRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final InventorySettingsService settingsService;
    private final InventoryItemService itemService;
    private final StockLedger stockLedger;
    private final InventoryAuditLogger auditLogger;

    public InventoryTransferService(
            InventoryTransferRepository transferRepository,
            InventoryTransferLineRepository lineRepository,
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            UserRepository userRepository,
            InventorySettingsService settingsService,
            InventoryItemService itemService,
            StockLedger stockLedger,
            InventoryAuditLogger auditLogger) {
        this.transferRepository = transferRepository;
        this.lineRepository = lineRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.itemService = itemService;
        this.stockLedger = stockLedger;
        this.auditLogger = auditLogger;
    }

    // ── reads ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransferResponse> list(TransferStatus status, Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<InventoryTransfer> page = status == null
                ? transferRepository.findAllTransfers(branchId, pageable)
                : transferRepository.findByStatus(branchId, status, pageable);
        return page.map(decorator(page.getContent()));
    }

    @Transactional(readOnly = true)
    public TransferResponse get(UUID id) {
        InventoryTransfer transfer = load(id);
        return decorator(List.of(transfer)).apply(transfer);
    }

    @Transactional(readOnly = true)
    public long countInTransit() {
        return transferRepository.countInTransit(BranchContext.get());
    }

    // ── writes ───────────────────────────────────────────────────────────

    /** Sends stock on its way: it leaves the source now and is owed to the destination. */
    public TransferResponse send(TransferRequest request) {
        UUID branchId = BranchContext.get();
        if (request.getFromNodeId().equals(request.getToNodeId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        loadNode(request.getFromNodeId(), branchId);
        InventoryNode target = loadNode(request.getToNodeId(), branchId);

        // One product may not appear twice: the person receiving would not know what to count.
        Set<UUID> seen = new HashSet<>();
        for (TransferRequest.Line line : request.getLines()) {
            if (!seen.add(line.getItemId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }

        InventoryTransfer transfer = InventoryTransfer.builder()
                .fromNodeId(request.getFromNodeId())
                .toNodeId(request.getToNodeId())
                .status(TransferStatus.IN_TRANSIT)
                .notes(request.getNotes())
                .sentBy(SecurityUtils.getCurrentUserId().orElse(null))
                .sentAt(Instant.now())
                .build();
        transfer.setBranchId(branchId);
        InventoryTransfer saved = transferRepository.save(transfer);

        for (TransferRequest.Line line : request.getLines()) {
            InventoryItem item = itemRepository
                    .findByIdAndBranchIdAndDeletedAtIsNull(line.getItemId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found: " + line.getItemId()));
            // The destination has to accept the category, and it has to be said now rather than
            // on arrival — otherwise the goods are already on the trolley when it turns out they
            // have nowhere to go.
            itemService.assertCanReceiveStock(item.getId(), target.getId());

            InventoryTransferLine entity = InventoryTransferLine.builder()
                    .transferId(saved.getId())
                    .itemId(item.getId())
                    .quantity(line.getQuantity())
                    .build();
            entity.setBranchId(branchId);
            lineRepository.save(entity);

            stockLedger.move(
                    item.getId(),
                    request.getFromNodeId(),
                    StockMovementType.TRANSFER_OUT,
                    line.getQuantity().negate(),
                    "TRANSFER",
                    saved.getId(),
                    "Transferə göndərildi");
        }

        auditLogger.log("CREATE", "INVENTORY_TRANSFER", saved.getId(), null,
                Map.of("from", request.getFromNodeId(), "to", request.getToNodeId(),
                        "lines", request.getLines().size()));
        return get(saved.getId());
    }

    /** Counts the goods in at the destination. */
    public TransferResponse receive(UUID id) {
        InventoryTransfer transfer = load(id);
        assertInTransit(transfer);

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (requiresDifferentReceiver(transfer.getBranchId())
                && transfer.getSentBy() != null
                && transfer.getSentBy().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.TRANSFER_SELF_RECEIPT);
        }

        for (InventoryTransferLine line : lineRepository.findByTransferIdAndDeletedAtIsNull(id)) {
            stockLedger.move(
                    line.getItemId(),
                    transfer.getToNodeId(),
                    StockMovementType.TRANSFER_IN,
                    line.getQuantity(),
                    "TRANSFER",
                    transfer.getId(),
                    "Transferdən qəbul edildi");
        }

        transfer.setStatus(TransferStatus.RECEIVED);
        transfer.setReceivedBy(currentUserId);
        transfer.setReceivedAt(Instant.now());
        auditLogger.log("BUSINESS", "INVENTORY_TRANSFER", transfer.getId(),
                Map.of("status", TransferStatus.IN_TRANSIT), Map.of("status", TransferStatus.RECEIVED));
        return get(transfer.getId());
    }

    /** Calls off a transfer still in transit; the stock goes back where it came from. */
    public TransferResponse cancel(UUID id) {
        InventoryTransfer transfer = load(id);
        assertInTransit(transfer);

        for (InventoryTransferLine line : lineRepository.findByTransferIdAndDeletedAtIsNull(id)) {
            stockLedger.move(
                    line.getItemId(),
                    transfer.getFromNodeId(),
                    StockMovementType.TRANSFER_IN,
                    line.getQuantity(),
                    "TRANSFER",
                    transfer.getId(),
                    "Transfer ləğv edildi");
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setCancelledBy(SecurityUtils.getCurrentUserId().orElse(null));
        transfer.setCancelledAt(Instant.now());
        auditLogger.log("BUSINESS", "INVENTORY_TRANSFER", transfer.getId(),
                Map.of("status", TransferStatus.IN_TRANSIT), Map.of("status", TransferStatus.CANCELLED));
        return get(transfer.getId());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private boolean requiresDifferentReceiver(UUID branchId) {
        return Boolean.TRUE.equals(
                settingsService.findOrDefaults(branchId).getTransferRequiresDifferentReceiver());
    }

    private void assertInTransit(InventoryTransfer transfer) {
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessException(ErrorCode.TRANSFER_NOT_IN_TRANSIT);
        }
    }

    private InventoryTransfer load(UUID id) {
        return transferRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory transfer not found: " + id));
    }

    private InventoryNode loadNode(UUID id, UUID branchId) {
        return nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory node not found: " + id));
    }

    /** Resolves every name a page of transfers needs in a handful of queries, not one per row. */
    private java.util.function.Function<InventoryTransfer, TransferResponse> decorator(
            List<InventoryTransfer> transfers) {
        List<UUID> transferIds = transfers.stream().map(InventoryTransfer::getId).toList();
        Map<UUID, List<InventoryTransferLine>> linesByTransfer = transferIds.isEmpty()
                ? Map.of()
                : lineRepository.findByTransferIdInAndDeletedAtIsNull(transferIds).stream()
                        .collect(Collectors.groupingBy(InventoryTransferLine::getTransferId));

        Map<UUID, InventoryItem> items = new HashMap<>();
        linesByTransfer.values().stream()
                .flatMap(List::stream)
                .map(InventoryTransferLine::getItemId)
                .distinct()
                .forEach(itemId -> itemRepository.findById(itemId).ifPresent(i -> items.put(itemId, i)));

        Map<UUID, String> nodeNames = new HashMap<>();
        nodeRepository
                .findAllById(transfers.stream()
                        .flatMap(t -> java.util.stream.Stream.of(t.getFromNodeId(), t.getToNodeId()))
                        .distinct()
                        .toList())
                .forEach(node -> nodeNames.put(node.getId(), node.getName()));

        Map<UUID, String> userNames = new HashMap<>();
        userRepository
                .findAllById(transfers.stream()
                        .flatMap(t -> java.util.stream.Stream.of(t.getSentBy(), t.getReceivedBy()))
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .forEach(user -> userNames.put(user.getId(), user.getFullName()));

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean strict = transfers.isEmpty() || requiresDifferentReceiver(transfers.get(0).getBranchId());

        return transfer -> TransferResponse.builder()
                .id(transfer.getId())
                .fromNodeId(transfer.getFromNodeId())
                .fromNodeName(nodeNames.get(transfer.getFromNodeId()))
                .toNodeId(transfer.getToNodeId())
                .toNodeName(nodeNames.get(transfer.getToNodeId()))
                .status(transfer.getStatus())
                .notes(transfer.getNotes())
                .sentBy(transfer.getSentBy())
                .sentByName(transfer.getSentBy() == null ? null : userNames.get(transfer.getSentBy()))
                .sentAt(transfer.getSentAt())
                .receivedBy(transfer.getReceivedBy())
                .receivedByName(
                        transfer.getReceivedBy() == null ? null : userNames.get(transfer.getReceivedBy()))
                .receivedAt(transfer.getReceivedAt())
                .cancelledAt(transfer.getCancelledAt())
                .canReceive(transfer.getStatus() == TransferStatus.IN_TRANSIT
                        && (!strict || !java.util.Objects.equals(transfer.getSentBy(), currentUserId)))
                .lines(linesByTransfer.getOrDefault(transfer.getId(), List.of()).stream()
                        .map(line -> {
                            InventoryItem item = items.get(line.getItemId());
                            return TransferResponse.Line.builder()
                                    .itemId(line.getItemId())
                                    .itemName(item == null ? null : item.getName())
                                    .itemSku(item == null ? null : item.getSku())
                                    .unit(item == null ? null : item.getUnit())
                                    .quantity(line.getQuantity())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .build();
    }

    /** Stock currently on a trolley for a product, so the card's numbers add up. */
    @Transactional(readOnly = true)
    public BigDecimal inTransitFor(UUID itemId) {
        BigDecimal quantity = lineRepository.inTransitQuantity(itemId);
        return quantity == null ? BigDecimal.ZERO : quantity;
    }
}
