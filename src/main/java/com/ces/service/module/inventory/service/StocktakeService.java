package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.StocktakeCountRequest;
import com.ces.service.module.inventory.dto.StocktakeOpenRequest;
import com.ces.service.module.inventory.dto.StocktakeResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.entity.InventoryStock;
import com.ces.service.module.inventory.entity.Stocktake;
import com.ces.service.module.inventory.entity.StocktakeLine;
import com.ces.service.module.inventory.enums.StocktakeStatus;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import com.ces.service.module.inventory.repository.InventoryStockRepository;
import com.ces.service.module.inventory.repository.StocktakeLineRepository;
import com.ces.service.module.inventory.repository.StocktakeRepository;
import com.ces.service.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventarizasiya — a blind count of one folder.
 *
 * <p>The system figure is frozen when the sheet opens and hidden while it is open. Both matter:
 * hidden, because shown the number people confirm it instead of counting; frozen, because a
 * stock-in halfway through the count would otherwise move the variance to something nobody
 * actually counted.
 *
 * <p>Closing produces one approval covering every variance. Per-line approvals were the obvious
 * alternative and the wrong one — two hundred separate requests turn the second pair of eyes into
 * a formality nobody reads.
 */
@Service
@Transactional
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeLineRepository lineRepository;
    private final InventoryStockRepository stockRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final StockLedger stockLedger;
    private final ApprovalService approvalService;
    private final InventoryAuditLogger auditLogger;

    // @Lazy on ApprovalService: it collects every executor, and the stocktake executor needs this
    // service back — the same cycle the inventory executors break.
    public StocktakeService(
            StocktakeRepository stocktakeRepository,
            StocktakeLineRepository lineRepository,
            InventoryStockRepository stockRepository,
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            UserRepository userRepository,
            StockLedger stockLedger,
            @Lazy ApprovalService approvalService,
            InventoryAuditLogger auditLogger) {
        this.stocktakeRepository = stocktakeRepository;
        this.lineRepository = lineRepository;
        this.stockRepository = stockRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.userRepository = userRepository;
        this.stockLedger = stockLedger;
        this.approvalService = approvalService;
        this.auditLogger = auditLogger;
    }

    // ── reads ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StocktakeResponse> list(StocktakeStatus status, Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<Stocktake> page = status == null
                ? stocktakeRepository.findAllStocktakes(branchId, pageable)
                : stocktakeRepository.findByStatus(branchId, status, pageable);
        // Summary rows only — the lines of every sheet on a page would be a lot of nothing.
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StocktakeResponse get(UUID id) {
        return toDetail(load(id));
    }

    // ── writes ───────────────────────────────────────────────────────────

    /** Opens a sheet listing everything the folder currently holds, figures frozen. */
    public StocktakeResponse open(StocktakeOpenRequest request) {
        UUID branchId = BranchContext.get();
        InventoryNode node = nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(request.getNodeId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory node not found: " + request.getNodeId()));

        stocktakeRepository.findActiveForNode(branchId, node.getId()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.STOCKTAKE_ALREADY_OPEN);
        });

        Stocktake stocktake = Stocktake.builder()
                .nodeId(node.getId())
                .status(StocktakeStatus.OPEN)
                .notes(request.getNotes())
                .openedBy(SecurityUtils.getCurrentUserId().orElse(null))
                .openedAt(Instant.now())
                .build();
        stocktake.setBranchId(branchId);
        Stocktake saved = stocktakeRepository.save(stocktake);

        for (InventoryStock stock : stockRepository.findByNodeIdAndDeletedAtIsNull(node.getId())) {
            StocktakeLine line = StocktakeLine.builder()
                    .stocktakeId(saved.getId())
                    .itemId(stock.getItemId())
                    .systemQuantity(stock.getQuantity())
                    .build();
            line.setBranchId(branchId);
            lineRepository.save(line);
        }

        auditLogger.log("CREATE", "INVENTORY_STOCKTAKE", saved.getId(), null,
                Map.of("nodeId", node.getId()));
        return toDetail(saved);
    }

    /** Records what was found for one product. Re-counting the same product overwrites it. */
    public StocktakeResponse count(UUID id, StocktakeCountRequest request) {
        Stocktake stocktake = load(id);
        assertOpen(stocktake);

        StocktakeLine line = lineRepository
                .findByStocktakeIdAndItemIdAndDeletedAtIsNull(id, request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stocktake line not found for item: " + request.getItemId()));

        line.setCountedQuantity(request.getCountedQuantity());
        line.setCountedBy(SecurityUtils.getCurrentUserId().orElse(null));
        line.setCountedAt(Instant.now());
        line.setNotes(request.getNotes());
        return toDetail(stocktake);
    }

    /**
     * Closes the sheet and parks every variance as a single approval.
     *
     * <p>Uncounted lines are left alone rather than treated as zero: nobody looked at them, and
     * writing off stock because a sheet was abandoned halfway would be the worst possible reading
     * of a blank.
     */
    public StocktakeResponse close(UUID id) {
        Stocktake stocktake = load(id);
        assertOpen(stocktake);

        List<StocktakeLine> lines = lineRepository.findByStocktakeIdAndDeletedAtIsNull(id);
        long variances = lines.stream()
                .filter(line -> line.getCountedQuantity() != null)
                .filter(line -> line.getCountedQuantity().compareTo(line.getSystemQuantity()) != 0)
                .count();

        if (variances == 0) {
            // Nothing to approve: the count agreed with the records, so the sheet just closes.
            stocktake.setStatus(StocktakeStatus.APPLIED);
            stocktake.setClosedBy(SecurityUtils.getCurrentUserId().orElse(null));
            stocktake.setClosedAt(Instant.now());
            stocktake.setAppliedAt(Instant.now());
            auditLogger.log("BUSINESS", "INVENTORY_STOCKTAKE", stocktake.getId(), null,
                    Map.of("result", "NO_VARIANCE"));
            return toDetail(stocktake);
        }

        StocktakeResponse snapshot = toDetail(stocktake);
        ApprovalRequestResponse approval = approvalService.submit(
                ApprovalEntityType.INVENTORY_STOCKTAKE,
                stocktake.getId(),
                nodeName(stocktake.getNodeId()) + " — sayım",
                ApprovalOperation.STOCKTAKE_APPLY,
                Map.of("stocktakeId", stocktake.getId(), "varianceCount", variances),
                snapshot);

        stocktake.setStatus(StocktakeStatus.PENDING_APPROVAL);
        stocktake.setApprovalRequestId(approval.id());
        stocktake.setClosedBy(SecurityUtils.getCurrentUserId().orElse(null));
        stocktake.setClosedAt(Instant.now());
        return toDetail(stocktake);
    }

    /**
     * Applies the counted figures. Called by the approval executor once a second person agrees —
     * never straight from a controller.
     */
    public void apply(UUID stocktakeId) {
        Stocktake stocktake = load(stocktakeId);
        if (stocktake.getStatus() != StocktakeStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.STOCKTAKE_NOT_OPEN);
        }
        for (StocktakeLine line : lineRepository.findByStocktakeIdAndDeletedAtIsNull(stocktakeId)) {
            if (line.getCountedQuantity() == null) {
                continue;
            }
            stockLedger.setAbsolute(
                    line.getItemId(),
                    stocktake.getNodeId(),
                    line.getCountedQuantity(),
                    "STOCKTAKE",
                    stocktake.getId(),
                    "İnventarizasiya");
        }
        stocktake.setStatus(StocktakeStatus.APPLIED);
        stocktake.setAppliedAt(Instant.now());
        auditLogger.log("BUSINESS", "INVENTORY_STOCKTAKE", stocktake.getId(), null,
                Map.of("result", "APPLIED"));
    }

    /** Abandons a sheet. Nothing counted is applied. */
    public StocktakeResponse cancel(UUID id) {
        Stocktake stocktake = load(id);
        if (stocktake.getStatus() == StocktakeStatus.APPLIED) {
            throw new BusinessException(ErrorCode.STOCKTAKE_NOT_OPEN);
        }
        stocktake.setStatus(StocktakeStatus.CANCELLED);
        auditLogger.log("BUSINESS", "INVENTORY_STOCKTAKE", stocktake.getId(), null,
                Map.of("result", "CANCELLED"));
        return toDetail(stocktake);
    }

    /** Marks a sheet cancelled when its approval was rejected. */
    public void markRejected(UUID stocktakeId) {
        stocktakeRepository.findById(stocktakeId).ifPresent(stocktake -> {
            if (stocktake.getStatus() == StocktakeStatus.PENDING_APPROVAL) {
                stocktake.setStatus(StocktakeStatus.CANCELLED);
            }
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void assertOpen(Stocktake stocktake) {
        if (stocktake.getStatus() != StocktakeStatus.OPEN) {
            throw new BusinessException(ErrorCode.STOCKTAKE_NOT_OPEN);
        }
    }

    private Stocktake load(UUID id) {
        return stocktakeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Stocktake not found: " + id));
    }

    private String nodeName(UUID nodeId) {
        return nodeRepository.findById(nodeId).map(InventoryNode::getName).orElse("Qovluq");
    }

    private StocktakeResponse toSummary(Stocktake stocktake) {
        List<StocktakeLine> lines = lineRepository.findByStocktakeIdAndDeletedAtIsNull(stocktake.getId());
        return baseBuilder(stocktake, lines).lines(List.of()).build();
    }

    private StocktakeResponse toDetail(Stocktake stocktake) {
        List<StocktakeLine> lines = lineRepository.findByStocktakeIdAndDeletedAtIsNull(stocktake.getId());
        Map<UUID, InventoryItem> items = new HashMap<>();
        itemRepository
                .findAllById(lines.stream().map(StocktakeLine::getItemId).distinct().toList())
                .forEach(item -> items.put(item.getId(), item));

        // The whole point of a blind count: while it is open, the recorded figure is not sent.
        boolean revealed = stocktake.getStatus() != StocktakeStatus.OPEN;

        return baseBuilder(stocktake, lines)
                .lines(lines.stream()
                        .map(line -> {
                            InventoryItem item = items.get(line.getItemId());
                            BigDecimal variance = revealed && line.getCountedQuantity() != null
                                    ? line.getCountedQuantity().subtract(line.getSystemQuantity())
                                    : null;
                            return StocktakeResponse.Line.builder()
                                    .itemId(line.getItemId())
                                    .itemName(item == null ? null : item.getName())
                                    .itemSku(item == null ? null : item.getSku())
                                    .unit(item == null ? null : item.getUnit())
                                    .systemQuantity(revealed ? line.getSystemQuantity() : null)
                                    .countedQuantity(line.getCountedQuantity())
                                    .variance(variance)
                                    .notes(line.getNotes())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .build();
    }

    private StocktakeResponse.StocktakeResponseBuilder baseBuilder(
            Stocktake stocktake, List<StocktakeLine> lines) {
        int counted = (int) lines.stream().filter(l -> l.getCountedQuantity() != null).count();
        int variances = stocktake.getStatus() == StocktakeStatus.OPEN
                ? 0
                : (int) lines.stream()
                        .filter(l -> l.getCountedQuantity() != null)
                        .filter(l -> l.getCountedQuantity().compareTo(l.getSystemQuantity()) != 0)
                        .count();
        String openedByName = stocktake.getOpenedBy() == null
                ? null
                : userRepository.findById(stocktake.getOpenedBy())
                        .map(u -> u.getFullName())
                        .orElse(null);

        return StocktakeResponse.builder()
                .id(stocktake.getId())
                .nodeId(stocktake.getNodeId())
                .nodeName(nodeName(stocktake.getNodeId()))
                .status(stocktake.getStatus())
                .notes(stocktake.getNotes())
                .approvalRequestId(stocktake.getApprovalRequestId())
                .openedBy(stocktake.getOpenedBy())
                .openedByName(openedByName)
                .openedAt(stocktake.getOpenedAt())
                .closedAt(stocktake.getClosedAt())
                .appliedAt(stocktake.getAppliedAt())
                .lineCount(lines.size())
                .countedCount(counted)
                .varianceCount(variances);
    }
}
