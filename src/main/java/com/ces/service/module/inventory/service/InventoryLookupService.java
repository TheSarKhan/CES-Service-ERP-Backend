package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryLookupResponse;
import com.ces.service.module.inventory.entity.InventoryItem;
import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryItemUnitRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a scanned QR/barcode value to whatever it identifies (Layer node, product, or
 * serialized unit) — backs the browser-camera scan flow (SRS "QR / Barkod Sistemi").
 */
@Service
@Transactional(readOnly = true)
public class InventoryLookupService {

    private final InventoryNodeRepository nodeRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryItemUnitRepository unitRepository;

    public InventoryLookupService(
            InventoryNodeRepository nodeRepository,
            InventoryItemRepository itemRepository,
            InventoryItemUnitRepository unitRepository) {
        this.nodeRepository = nodeRepository;
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
    }

    public InventoryLookupResponse resolve(String code) {
        UUID branchId = BranchContext.get();

        InventoryNode node = nodeRepository
                .findByBranchIdAndQrCodeAndDeletedAtIsNull(branchId, code)
                .orElseGet(() -> nodeRepository.findByBranchIdAndBarcodeAndDeletedAtIsNull(branchId, code).orElse(null));
        if (node != null) {
            return InventoryLookupResponse.builder().type("NODE").id(node.getId()).build();
        }

        InventoryItem item = itemRepository
                .findByBranchIdAndQrCodeAndDeletedAtIsNull(branchId, code)
                .orElseGet(() -> itemRepository.findByBranchIdAndBarcodeAndDeletedAtIsNull(branchId, code).orElse(null));
        if (item != null) {
            return InventoryLookupResponse.builder().type("ITEM").id(item.getId()).build();
        }

        InventoryItemUnit unit = unitRepository
                .findByBranchIdAndQrCodeAndDeletedAtIsNull(branchId, code)
                .orElseGet(() -> unitRepository
                        .findByBranchIdAndBarcodeAndDeletedAtIsNull(branchId, code)
                        .orElseGet(() -> unitRepository.findByBranchIdAndSerialNumberAndDeletedAtIsNull(branchId, code).orElse(null)));
        if (unit != null) {
            return InventoryLookupResponse.builder().type("ITEM_UNIT").id(unit.getId()).build();
        }

        throw new ResourceNotFoundException("No node, item or unit matches code: " + code);
    }
}
