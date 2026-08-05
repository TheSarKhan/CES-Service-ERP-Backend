package com.ces.service.module.inventory.service;

import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.StockAlertSummaryResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.StockLevelCounts;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Products that have run low.
 *
 * <p>Levels are decided by {@link StockClock} everywhere — badge, filter, counts and the daily
 * digest — so a product cannot read "low" on one screen and "fine" on another.
 */
@Service
@Transactional(readOnly = true)
public class StockAlertService {

    private final InventoryItemRepository itemRepository;
    private final InventoryItemService itemService;
    private final InventoryLotService lotService;

    public StockAlertService(
            InventoryItemRepository itemRepository,
            InventoryItemService itemService,
            InventoryLotService lotService) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.lotService = lotService;
    }

    public StockAlertSummaryResponse summary() {
        return summaryFor(BranchContext.get());
    }

    public StockAlertSummaryResponse summaryFor(UUID branchId) {
        // [expiring soon, already expired] — the batch side of "needs attention today".
        long[] lots = lotService.expirySummaryFor(branchId);
        StockLevelCounts counts = itemRepository.countStockLevels(branchId);
        if (counts == null) {
            return StockAlertSummaryResponse.of(0, 0, lots[0], lots[1]);
        }
        return StockAlertSummaryResponse.of(
                counts.getLow() == null ? 0 : counts.getLow(),
                counts.getCritical() == null ? 0 : counts.getCritical(),
                lots[0],
                lots[1]);
    }

    /** Low-stock listing, worst shortfall first. */
    public Page<InventoryItemResponse> list(boolean criticalOnly, Pageable pageable) {
        return page(BranchContext.get(), criticalOnly, pageable);
    }

    /** Same listing without a request context — the scheduled digest runs outside one. */
    public Page<InventoryItemResponse> listFor(UUID branchId, boolean criticalOnly, Pageable pageable) {
        return page(branchId, criticalOnly, pageable);
    }

    /**
     * Worst shortfall first — how far the total has fallen past the line that matters for that
     * product, so the one closest to stopping work is on top.
     *
     * <p>{@code JpaSort.unsafe} because this is an expression rather than a column, and it lives
     * here rather than inside the query's own ORDER BY: Spring appends the Pageable's sort after
     * the query's, so a hardcoded one would quietly outrank whichever header the user clicked.
     */
    public static final Sort SHORTFALL_FIRST = JpaSort.unsafe(
            Sort.Direction.ASC,
            "(coalesce((select sum(s.quantity) from ces_service.inventory_stock s"
                    + " where s.item_id = i.id and s.deleted_at is null), 0)"
                    + " - coalesce(i.critical_quantity, i.min_quantity))");

    private Page<InventoryItemResponse> page(UUID branchId, boolean criticalOnly, Pageable pageable) {
        Page<InventoryItem> page = itemRepository.findLowStock(branchId, criticalOnly, pageable);
        List<InventoryItem> content = page.getContent();
        return page.map(itemService.stockDecorator(content));
    }
}
