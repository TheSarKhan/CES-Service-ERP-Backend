package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.entity.InventoryStock;
import com.ces.service.module.inventory.entity.StockMovement;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.repository.InventoryStockRepository;
import com.ces.service.module.inventory.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place stock quantities change.
 *
 * <p>Every caller — stock-in, stock-out, count corrections, transfers, unit registration — goes
 * through here, so every change leaves a ledger line behind. Letting services write
 * {@code inventory_stock} directly is exactly how a history ends up with holes in it, and a history
 * with holes is worse than none: it looks authoritative.
 */
@Service
@Transactional
public class StockLedger {

    private final InventoryStockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    public StockLedger(
            InventoryStockRepository stockRepository, StockMovementRepository movementRepository) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
    }

    /**
     * Moves a location's quantity by a signed amount and records it.
     *
     * @param delta positive brings stock in, negative takes it out
     * @throws BusinessException with {@link ErrorCode#STOCK_INSUFFICIENT} if the result is negative
     */
    public StockMovement move(
            UUID itemId,
            UUID nodeId,
            StockMovementType type,
            BigDecimal delta,
            String referenceType,
            UUID referenceId,
            String reason) {
        if (delta == null || delta.signum() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        InventoryStock stock = lockOrCreate(itemId, nodeId);
        BigDecimal next = stock.getQuantity().add(delta);
        if (next.signum() < 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        stock.setQuantity(next);
        return record(itemId, nodeId, null, type, delta, next, referenceType, referenceId, reason);
    }

    /**
     * Sets a location to an absolute quantity — what a count correction does. The ledger still
     * stores the difference, because "what changed" is the question a history has to answer;
     * "what it became" is already in {@code balanceAfter}.
     *
     * @return the recorded movement, or null when the count matched and nothing moved
     */
    public StockMovement setAbsolute(
            UUID itemId,
            UUID nodeId,
            BigDecimal counted,
            String referenceType,
            UUID referenceId,
            String reason) {
        if (counted == null || counted.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        InventoryStock stock = lockOrCreate(itemId, nodeId);
        BigDecimal delta = counted.subtract(stock.getQuantity());
        stock.setQuantity(counted);
        if (delta.signum() == 0) {
            // No movement row: a count that confirmed the number is not a stock event, and logging
            // it would bury the real corrections in noise.
            return null;
        }
        return record(
                itemId,
                nodeId,
                null,
                StockMovementType.ADJUST,
                delta,
                counted,
                referenceType,
                referenceId,
                reason);
    }

    /**
     * Rewrites a serialized product's quantity at a location from its units.
     *
     * <p>Units are the truth for a serialized product, so this recomputes rather than increments —
     * a counter that drifts is harder to notice than one that is simply wrong once.
     */
    public void syncSerialized(
            UUID itemId, UUID nodeId, long unitCount, UUID unitId, StockMovementType type, String reason) {
        InventoryStock stock = lockOrCreate(itemId, nodeId);
        BigDecimal counted = BigDecimal.valueOf(unitCount);
        BigDecimal delta = counted.subtract(stock.getQuantity());
        stock.setQuantity(counted);
        if (delta.signum() == 0) {
            return;
        }
        record(itemId, nodeId, unitId, type, delta, counted, "UNIT", unitId, reason);
    }

    // ── reads ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BigDecimal totalFor(UUID itemId) {
        BigDecimal total = stockRepository.totalForItem(itemId);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional(readOnly = true)
    public List<InventoryStock> locationsOf(UUID itemId) {
        return stockRepository.findByItemIdAndDeletedAtIsNull(itemId);
    }

    /** Totals for a whole page of products in one query. */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> totalsFor(List<UUID> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return stockRepository.totalsForItems(itemIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (BigDecimal) row[1]));
    }

    /** Locations for a whole page of products, grouped by product. */
    @Transactional(readOnly = true)
    public Map<UUID, List<InventoryStock>> locationsFor(List<UUID> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return stockRepository.findByItemIds(itemIds).stream()
                .collect(Collectors.groupingBy(InventoryStock::getItemId));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * A product first arriving at a folder has no row yet, so one is created at zero and then
     * moved. Creating it here keeps every caller from having to know that.
     */
    private InventoryStock lockOrCreate(UUID itemId, UUID nodeId) {
        return stockRepository
                .findForUpdate(itemId, nodeId)
                .orElseGet(() -> {
                    InventoryStock created = InventoryStock.builder()
                            .itemId(itemId)
                            .nodeId(nodeId)
                            .quantity(BigDecimal.ZERO)
                            .build();
                    created.setBranchId(BranchContext.get());
                    return stockRepository.save(created);
                });
    }

    private StockMovement record(
            UUID itemId,
            UUID nodeId,
            UUID unitId,
            StockMovementType type,
            BigDecimal delta,
            BigDecimal balanceAfter,
            String referenceType,
            UUID referenceId,
            String reason) {
        StockMovement movement = StockMovement.builder()
                .itemId(itemId)
                .nodeId(nodeId)
                .unitId(unitId)
                .movementType(type)
                .quantity(delta)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .build();
        movement.setBranchId(BranchContext.get());
        return movementRepository.save(movement);
    }
}
