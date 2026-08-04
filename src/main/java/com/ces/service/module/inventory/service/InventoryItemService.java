package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryItemRequest;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.StockLocationResponse;
import com.ces.service.module.inventory.entity.InventoryCategory;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.entity.InventoryStock;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.repository.InventoryCategoryRepository;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import com.ces.service.module.inventory.repository.InventoryStockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
 * Product (Məhsul) management.
 *
 * <p>Business rules:
 * <ul>
 *   <li>SKU unique within branch → {@link ErrorCode#DUPLICATE_SKU}.</li>
 *   <li>A node may hold items directly and have child nodes at the same time — a shelf can carry
 *       loose products and boxes side by side.</li>
 *   <li>Location and quantity live in {@code inventory_stock}, one row per folder, so the same
 *       product can be held in several places; serialized items must go through
 *       {@code InventoryItemUnitService} instead → {@link ErrorCode#ITEM_IS_SERIALIZED}.</li>
 *   <li>Stock cannot go negative → {@link ErrorCode#STOCK_INSUFFICIENT}.</li>
 * </ul>
 */
@Service
@Transactional
public class InventoryItemService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final InventoryCategoryRepository categoryRepository;
    private final InventoryStockRepository stockRepository;
    private final StockLedger stockLedger;
    private final InventoryAuditLogger auditLogger;

    public InventoryItemService(
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            InventoryCategoryRepository categoryRepository,
            InventoryStockRepository stockRepository,
            StockLedger stockLedger,
            InventoryAuditLogger auditLogger) {
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.categoryRepository = categoryRepository;
        this.stockRepository = stockRepository;
        this.stockLedger = stockLedger;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> list(UUID categoryId, UUID nodeId, String search, Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<InventoryItem> page =
                itemRepository.search(branchId, categoryId, nodeId, toLikePattern(search), pageable);
        return page.map(withStock(page.getContent()));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(UUID id) {
        return describe(loadItem(id));
    }

    /** Distinct categories actually present at a node — powers the per-category sections. */
    @Transactional(readOnly = true)
    public List<UUID> listCategoryIdsAtNode(UUID nodeId) {
        UUID branchId = BranchContext.get();
        return stockRepository.findDistinctCategoryIdsAtNode(branchId, nodeId);
    }

    public InventoryItemResponse create(InventoryItemRequest request) {
        UUID branchId = BranchContext.get();

        InventoryNode node = loadNode(request.getNodeId(), branchId);
        loadCategory(request.getCategoryId(), branchId);
        assertCategoryAllowed(node, request.getCategoryId());

        if (itemRepository.existsByBranchIdAndSkuAndDeletedAtIsNull(branchId, request.getSku())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }

        InventoryItem item = InventoryItem.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .unit(request.getUnit())
                .purchasePrice(request.getPurchasePrice())
                .isSerialized(isSerializedRequest(request))
                .attributes(toJson(request.getAttributes()))
                .warrantyMonths(request.getWarrantyMonths())
                .warrantyStartDate(request.getWarrantyStartDate())
                .warrantyEndDate(resolveWarrantyEnd(request, isSerializedRequest(request)))
                .supplier(request.getSupplier())
                .notes(request.getNotes())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();
        item.setBranchId(branchId);
        item.setQrCode(UUID.randomUUID().toString());

        InventoryItem saved = itemRepository.save(item);

        // The opening quantity is a real stock event, so it goes through the ledger like any
        // other. A serialized product opens at zero — its units bring the stock in themselves.
        BigDecimal opening = isSerializedRequest(request) ? BigDecimal.ZERO : request.getQuantity();
        if (opening != null && opening.signum() > 0) {
            stockLedger.move(
                    saved.getId(),
                    node.getId(),
                    StockMovementType.IN,
                    opening,
                    "ITEM_CREATE",
                    saved.getId(),
                    "İlkin qalıq");
        } else {
            // No opening stock, but the product still has to appear in the folder it was filed
            // under — otherwise it is created into thin air.
            ensureLocation(saved.getId(), node.getId(), branchId);
        }

        InventoryItemResponse response = describe(saved);
        auditLogger.log("CREATE", "INVENTORY_ITEM", saved.getId(), null, response);
        return response;
    }

    /** Updates product fields — does not relocate the item; use {@link #move} for that. */
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        InventoryItem item = loadItem(id);

        if (!item.getCategoryId().equals(request.getCategoryId())) {
            loadCategory(request.getCategoryId(), item.getBranchId());
            // Recategorising can strand the product in a folder that refuses the new category, so
            // every folder currently holding it has to accept it.
            for (InventoryStock stock : stockRepository.findByItemIdAndDeletedAtIsNull(item.getId())) {
                assertCategoryAllowed(loadNode(stock.getNodeId(), item.getBranchId()), request.getCategoryId());
            }
        }
        if (!item.getSku().equals(request.getSku())
                && itemRepository.existsByBranchIdAndSkuAndDeletedAtIsNullAndIdNot(
                        item.getBranchId(), request.getSku(), item.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }

        item.setCategoryId(request.getCategoryId());
        item.setName(request.getName());
        item.setSku(request.getSku());
        item.setBarcode(request.getBarcode());
        item.setUnit(request.getUnit());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setAttributes(toJson(request.getAttributes()));
        item.setWarrantyMonths(request.getWarrantyMonths());
        item.setWarrantyStartDate(request.getWarrantyStartDate());
        item.setWarrantyEndDate(resolveWarrantyEnd(request, item.getIsSerialized()));
        item.setSupplier(request.getSupplier());
        item.setNotes(request.getNotes());
        if (request.getIsActive() != null) {
            item.setIsActive(request.getIsActive());
        }
        // Location, quantity and isSerialized are intentionally untouched: they change only
        // through stock operations, moves and unit-level operations.
        return describe(item);
    }

    public void delete(UUID id) {
        InventoryItem item = loadItem(id);
        if (stockLedger.totalFor(item.getId()).signum() > 0) {
            throw new BusinessException(ErrorCode.ITEM_HAS_STOCK);
        }
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
        auditLogger.log("DELETE", "INVENTORY_ITEM", item.getId(), null, null);
    }

    /**
     * Moves everything held at one folder to another ("Məhsulu başqa yerə köçür").
     *
     * <p>Recorded as two ledger lines rather than one edited row: the stock genuinely left one
     * shelf and arrived at another, and a history that hides that cannot explain a later count.
     */
    public InventoryItemResponse move(UUID id, UUID fromNodeId, UUID toNodeId) {
        InventoryItem item = loadItem(id);
        if (fromNodeId.equals(toNodeId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        InventoryNode target = loadNode(toNodeId, item.getBranchId());
        assertCategoryAllowed(target, item.getCategoryId());

        InventoryStock source = stockRepository
                .findByItemIdAndNodeIdAndDeletedAtIsNull(item.getId(), fromNodeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory stock not found for node: " + fromNodeId));
        BigDecimal moving = source.getQuantity();

        if (moving.signum() > 0) {
            stockLedger.move(item.getId(), fromNodeId, StockMovementType.TRANSFER_OUT,
                    moving.negate(), "ITEM_MOVE", item.getId(), null);
            stockLedger.move(item.getId(), toNodeId, StockMovementType.TRANSFER_IN,
                    moving, "ITEM_MOVE", item.getId(), null);
        } else {
            // Nothing on the shelf, but the product was still filed there — carry the placement
            // over so it doesn't vanish from both folders.
            ensureLocation(item.getId(), toNodeId, item.getBranchId());
        }
        source.setDeletedAt(Instant.now());
        stockRepository.save(source);

        auditLogger.log("BUSINESS", "INVENTORY_ITEM", item.getId(),
                Map.of("nodeId", fromNodeId), Map.of("nodeId", toNodeId, "quantity", moving));
        return describe(item);
    }

    /** Stock-in at one folder ("Miqdarı artır"). */
    public InventoryItemResponse increaseQuantity(UUID id, UUID nodeId, BigDecimal delta, String reason) {
        InventoryItem item = loadNonSerializedItem(id);
        requirePositive(delta);
        assertNodeUsable(item, nodeId);
        stockLedger.move(item.getId(), nodeId, StockMovementType.IN, delta, "APPROVAL", null, reason);
        return describe(item);
    }

    /** Stock-out from one folder ("Miqdarı azalt"). */
    public InventoryItemResponse decreaseQuantity(UUID id, UUID nodeId, BigDecimal delta, String reason) {
        InventoryItem item = loadNonSerializedItem(id);
        requirePositive(delta);
        stockLedger.move(item.getId(), nodeId, StockMovementType.OUT, delta.negate(), "APPROVAL", null, reason);
        return describe(item);
    }

    /** Count correction at one folder ("Sayım fərqi"). */
    public InventoryItemResponse adjustQuantity(UUID id, UUID nodeId, BigDecimal newQuantity, String reason) {
        InventoryItem item = loadNonSerializedItem(id);
        if (newQuantity == null || newQuantity.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        assertNodeUsable(item, nodeId);
        stockLedger.setAbsolute(item.getId(), nodeId, newQuantity, "APPROVAL", null, reason);
        return describe(item);
    }

    /**
     * Rejects an impossible stock destination before the request is ever parked.
     *
     * <p>A pending request locks the product, so queuing one that can only fail at approval time
     * would block every other change to it until somebody rejects it — and the person who made the
     * mistake would never learn of it. Controllers call this first.
     */
    @Transactional(readOnly = true)
    public void assertCanReceiveStock(UUID itemId, UUID nodeId) {
        InventoryItem item = loadItem(itemId);
        assertNodeUsable(item, nodeId);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** One product with its stock rows resolved. */
    private InventoryItemResponse describe(InventoryItem item) {
        List<InventoryStock> stocks = stockRepository.findByItemIdAndDeletedAtIsNull(item.getId());
        return InventoryItemResponse.from(item, sum(stocks), toLocations(stocks));
    }

    /**
     * Builds a mapper that resolves stock for a whole page in two queries instead of two per row —
     * a 20-row listing was otherwise 40 extra round trips.
     */
    private java.util.function.Function<InventoryItem, InventoryItemResponse> withStock(List<InventoryItem> items) {
        List<UUID> ids = items.stream().map(InventoryItem::getId).toList();
        Map<UUID, List<InventoryStock>> byItem = stockLedger.locationsFor(ids);
        Map<UUID, String> nodeNames = nodeNamesFor(byItem.values().stream().flatMap(List::stream).toList());
        return item -> {
            List<InventoryStock> stocks = byItem.getOrDefault(item.getId(), List.of());
            return InventoryItemResponse.from(item, sum(stocks), toLocations(stocks, nodeNames));
        };
    }

    private BigDecimal sum(List<InventoryStock> stocks) {
        return stocks.stream().map(InventoryStock::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<StockLocationResponse> toLocations(List<InventoryStock> stocks) {
        return toLocations(stocks, nodeNamesFor(stocks));
    }

    /** Sorted by name so the same product's locations don't reshuffle between requests. */
    private List<StockLocationResponse> toLocations(List<InventoryStock> stocks, Map<UUID, String> nodeNames) {
        return stocks.stream()
                .map(stock -> StockLocationResponse.builder()
                        .nodeId(stock.getNodeId())
                        .nodeName(nodeNames.get(stock.getNodeId()))
                        .quantity(stock.getQuantity())
                        .build())
                .sorted(Comparator.comparing(
                        StockLocationResponse::getNodeName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private Map<UUID, String> nodeNamesFor(List<InventoryStock> stocks) {
        List<UUID> nodeIds = stocks.stream().map(InventoryStock::getNodeId).distinct().toList();
        if (nodeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        nodeRepository.findAllById(nodeIds).forEach(node -> names.put(node.getId(), node.getName()));
        return names;
    }

    /** Creates an empty placement so a product shows up in a folder before any stock arrives. */
    private void ensureLocation(UUID itemId, UUID nodeId, UUID branchId) {
        if (stockRepository.findByItemIdAndNodeIdAndDeletedAtIsNull(itemId, nodeId).isPresent()) {
            return;
        }
        InventoryStock stock = InventoryStock.builder()
                .itemId(itemId)
                .nodeId(nodeId)
                .quantity(BigDecimal.ZERO)
                .build();
        stock.setBranchId(branchId);
        stockRepository.save(stock);
    }

    /**
     * Bringing stock into a folder is also filing the product there, so the folder's category
     * rules apply — otherwise a restricted shelf could be filled through the back door.
     */
    private void assertNodeUsable(InventoryItem item, UUID nodeId) {
        assertCategoryAllowed(loadNode(nodeId, item.getBranchId()), item.getCategoryId());
    }


    private boolean isSerializedRequest(InventoryItemRequest request) {
        return request.getIsSerialized() != null && request.getIsSerialized();
    }

    /**
     * A serialized item keeps no end date of its own — its warranty is per unit, and storing a
     * second answer here would let the two drift apart. For everything else the explicit end date
     * wins, falling back to start + {@code warrantyMonths} so callers can give either.
     */
    private LocalDate resolveWarrantyEnd(InventoryItemRequest request, boolean serialized) {
        if (serialized) {
            return null;
        }
        if (request.getWarrantyEndDate() != null) {
            return request.getWarrantyEndDate();
        }
        return WarrantyClock.endDateFrom(request.getWarrantyStartDate(), request.getWarrantyMonths());
    }

    private InventoryItem loadNonSerializedItem(UUID id) {
        InventoryItem item = loadItem(id);
        if (Boolean.TRUE.equals(item.getIsSerialized())) {
            throw new BusinessException(ErrorCode.ITEM_IS_SERIALIZED);
        }
        return item;
    }

    private void requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    /** A node with no assigned categories is unrestricted — any category is allowed there. */
    private void assertCategoryAllowed(InventoryNode node, UUID categoryId) {
        Set<InventoryCategory> allowed = node.getAllowedCategories();
        if (!allowed.isEmpty() && allowed.stream().noneMatch(c -> c.getId().equals(categoryId))) {
            throw new BusinessException(ErrorCode.NODE_CATEGORY_NOT_ALLOWED);
        }
    }

    private InventoryItem loadItem(UUID id) {
        UUID branchId = BranchContext.get();
        return itemRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    private InventoryNode loadNode(UUID id, UUID branchId) {
        return nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory node not found: " + id));
    }

    private InventoryCategory loadCategory(UUID id, UUID branchId) {
        return categoryRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category not found: " + id));
    }

    /** See {@code InventoryItemRepository.search} javadoc for why this is built in Java. */
    private String toLikePattern(String search) {
        return (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";
    }

    private String toJson(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(attributes);
        } catch (Exception e) {
            return "{}";
        }
    }
}
