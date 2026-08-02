package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryItemRequest;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.entity.InventoryCategory;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.repository.InventoryCategoryRepository;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 *   <li>Items may only be placed on a leaf node (no children) → {@link ErrorCode#NODE_NOT_LEAF}.</li>
 *   <li>Non-serialized items track stock via the {@code quantity} column directly; serialized
 *       items must go through {@code InventoryItemUnitService} instead →
 *       {@link ErrorCode#ITEM_IS_SERIALIZED}.</li>
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
    private final InventoryAuditLogger auditLogger;

    public InventoryItemService(
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            InventoryCategoryRepository categoryRepository,
            InventoryAuditLogger auditLogger) {
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> list(UUID categoryId, UUID nodeId, String search, Pageable pageable) {
        UUID branchId = BranchContext.get();
        return itemRepository.search(branchId, categoryId, nodeId, toLikePattern(search), pageable)
                .map(InventoryItemResponse::from);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(UUID id) {
        return InventoryItemResponse.from(loadItem(id));
    }

    public InventoryItemResponse create(InventoryItemRequest request) {
        UUID branchId = BranchContext.get();

        InventoryNode node = loadNode(request.getNodeId(), branchId);
        assertIsLeaf(node.getId());
        loadCategory(request.getCategoryId(), branchId);
        assertCategoryAllowed(node, request.getCategoryId());

        if (itemRepository.existsByBranchIdAndSkuAndDeletedAtIsNull(branchId, request.getSku())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }

        InventoryItem item = InventoryItem.builder()
                .nodeId(node.getId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .isSerialized(request.getIsSerialized() == null ? Boolean.FALSE : request.getIsSerialized())
                .attributes(toJson(request.getAttributes()))
                .notes(request.getNotes())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();
        item.setBranchId(branchId);
        item.setQrCode(UUID.randomUUID().toString());

        InventoryItem saved = itemRepository.save(item);
        InventoryItemResponse response = InventoryItemResponse.from(saved);
        auditLogger.log("CREATE", "INVENTORY_ITEM", saved.getId(), null, response);
        return response;
    }

    /** Updates product fields — does not relocate the item; use {@link #move} for that. */
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        InventoryItem item = loadItem(id);

        if (!item.getCategoryId().equals(request.getCategoryId())) {
            loadCategory(request.getCategoryId(), item.getBranchId());
        }
        assertCategoryAllowed(loadNode(item.getNodeId(), item.getBranchId()), request.getCategoryId());
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
        item.setNotes(request.getNotes());
        if (request.getIsActive() != null) {
            item.setIsActive(request.getIsActive());
        }
        // quantity / isSerialized are intentionally not touched here — they change only through
        // the dedicated stock operations below (and unit-level operations for serialized items).
        return InventoryItemResponse.from(item);
    }

    public void delete(UUID id) {
        InventoryItem item = loadItem(id);
        if (!item.getIsSerialized() && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ErrorCode.ITEM_HAS_STOCK);
        }
        // TODO(inventory-units): also block when serialized items still have active units.
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
        auditLogger.log("DELETE", "INVENTORY_ITEM", item.getId(), null, null);
    }

    /** Relocates a product to a different leaf node ("Məhsulu başqa yerə köçür"). */
    public InventoryItemResponse move(UUID id, UUID newNodeId) {
        InventoryItem item = loadItem(id);
        UUID oldNodeId = item.getNodeId();
        InventoryNode newNode = loadNode(newNodeId, item.getBranchId());
        assertIsLeaf(newNode.getId());
        assertCategoryAllowed(newNode, item.getCategoryId());
        item.setNodeId(newNode.getId());
        auditLogger.log(
                "BUSINESS", "INVENTORY_ITEM", item.getId(), Map.of("nodeId", oldNodeId), Map.of("nodeId", newNode.getId()));
        return InventoryItemResponse.from(item);
    }

    /** Stock-in for non-serialized items ("Miqdarı artır"). */
    public InventoryItemResponse increaseQuantity(UUID id, BigDecimal delta) {
        InventoryItem item = loadNonSerializedItem(id);
        requirePositive(delta);
        BigDecimal before = item.getQuantity();
        item.setQuantity(before.add(delta));
        auditLogger.log(
                "BUSINESS", "INVENTORY_ITEM", item.getId(), Map.of("quantity", before), Map.of("quantity", item.getQuantity()));
        return InventoryItemResponse.from(item);
    }

    /** Stock-out for non-serialized items ("Miqdarı azalt"). */
    public InventoryItemResponse decreaseQuantity(UUID id, BigDecimal delta) {
        InventoryItem item = loadNonSerializedItem(id);
        requirePositive(delta);
        if (item.getQuantity().compareTo(delta) < 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        BigDecimal before = item.getQuantity();
        item.setQuantity(before.subtract(delta));
        auditLogger.log(
                "BUSINESS", "INVENTORY_ITEM", item.getId(), Map.of("quantity", before), Map.of("quantity", item.getQuantity()));
        return InventoryItemResponse.from(item);
    }

    /** Inventory-count correction for non-serialized items ("Sayım fərqi"). */
    public InventoryItemResponse adjustQuantity(UUID id, BigDecimal newQuantity) {
        InventoryItem item = loadNonSerializedItem(id);
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        BigDecimal before = item.getQuantity();
        item.setQuantity(newQuantity);
        auditLogger.log(
                "BUSINESS", "INVENTORY_ITEM", item.getId(), Map.of("quantity", before), Map.of("quantity", newQuantity));
        return InventoryItemResponse.from(item);
    }

    // ── helpers ──────────────────────────────────────────────────────────

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

    private void assertIsLeaf(UUID nodeId) {
        if (nodeRepository.existsByParentIdAndDeletedAtIsNull(nodeId)) {
            throw new BusinessException(ErrorCode.NODE_NOT_LEAF);
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
