package com.ces.service.module.inventory.service;

import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.StockMovementResponse;
import com.ces.service.module.inventory.entity.StockMovement;
import com.ces.service.module.inventory.enums.StockMovementType;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import com.ces.service.module.inventory.repository.StockMovementRepository;
import com.ces.service.module.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the stock ledger.
 *
 * <p>Write access belongs to {@link StockLedger} alone; this side only resolves the ids a movement
 * carries into names, because a history that reads "3c63… → −5" answers nothing.
 */
@Service
@Transactional(readOnly = true)
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryNodeRepository nodeRepository;
    private final UserRepository userRepository;

    public StockMovementService(
            StockMovementRepository movementRepository,
            InventoryItemRepository itemRepository,
            InventoryNodeRepository nodeRepository,
            UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.userRepository = userRepository;
    }

    public Page<StockMovementResponse> search(
            UUID itemId, UUID nodeId, StockMovementType type, Pageable pageable) {
        UUID branchId = BranchContext.get();
        Page<StockMovement> page = type == null
                ? movementRepository.search(branchId, itemId, nodeId, pageable)
                : movementRepository.searchByType(branchId, itemId, nodeId, type, pageable);
        return page.map(decorator(page.getContent()));
    }

    /** Resolves every name a page needs in three queries rather than three per row. */
    private java.util.function.Function<StockMovement, StockMovementResponse> decorator(
            List<StockMovement> movements) {
        Map<UUID, String> itemNames = new HashMap<>();
        itemRepository
                .findAllById(movements.stream().map(StockMovement::getItemId).distinct().toList())
                .forEach(item -> itemNames.put(item.getId(), item.getName()));

        Map<UUID, String> nodeNames = new HashMap<>();
        nodeRepository
                .findAllById(movements.stream().map(StockMovement::getNodeId).distinct().toList())
                .forEach(node -> nodeNames.put(node.getId(), node.getName()));

        Map<UUID, String> userNames = new HashMap<>();
        userRepository
                .findAllById(movements.stream()
                        .map(StockMovement::getCreatedBy)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .forEach(user -> userNames.put(user.getId(), user.getFullName()));

        return movement -> StockMovementResponse.from(
                movement,
                itemNames.get(movement.getItemId()),
                nodeNames.get(movement.getNodeId()),
                movement.getCreatedBy() == null ? null : userNames.get(movement.getCreatedBy()));
    }
}
