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

    public StockAlertService(InventoryItemRepository itemRepository, InventoryItemService itemService) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    public StockAlertSummaryResponse summary() {
        return summaryFor(BranchContext.get());
    }

    public StockAlertSummaryResponse summaryFor(UUID branchId) {
        StockLevelCounts counts = itemRepository.countStockLevels(branchId);
        if (counts == null) {
            return StockAlertSummaryResponse.of(0, 0);
        }
        return StockAlertSummaryResponse.of(
                counts.getLow() == null ? 0 : counts.getLow(),
                counts.getCritical() == null ? 0 : counts.getCritical());
    }

    /** Low-stock listing, worst shortfall first. */
    public Page<InventoryItemResponse> list(boolean criticalOnly, Pageable pageable) {
        return page(BranchContext.get(), criticalOnly, pageable);
    }

    /** Same listing without a request context — the scheduled digest runs outside one. */
    public Page<InventoryItemResponse> listFor(UUID branchId, boolean criticalOnly, Pageable pageable) {
        return page(branchId, criticalOnly, pageable);
    }

    private Page<InventoryItemResponse> page(UUID branchId, boolean criticalOnly, Pageable pageable) {
        Page<InventoryItem> page = itemRepository.findLowStock(branchId, criticalOnly, pageable);
        List<InventoryItem> content = page.getContent();
        return page.map(itemService.stockDecorator(content));
    }
}
