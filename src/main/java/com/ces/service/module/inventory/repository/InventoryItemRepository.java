package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    boolean existsByBranchIdAndSkuAndDeletedAtIsNull(UUID branchId, String sku);

    boolean existsByBranchIdAndSkuAndDeletedAtIsNullAndIdNot(UUID branchId, String sku, UUID excludeId);

    boolean existsByNodeIdAndDeletedAtIsNull(UUID nodeId);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    Optional<InventoryItem> findByBranchIdAndQrCodeAndDeletedAtIsNull(UUID branchId, String qrCode);

    Optional<InventoryItem> findByBranchIdAndBarcodeAndDeletedAtIsNull(UUID branchId, String barcode);

    /**
     * Filtered + searchable listing. Any of the filter params may be null (ignored when null).
     * {@code searchPattern} matches name / SKU / barcode (case-insensitive) — callers must
     * pre-build it as {@code "%" + search.toLowerCase() + "%"} (or pass null). Building the
     * wildcard pattern in Java rather than via SQL {@code concat()} avoids a PgJDBC quirk where
     * {@code '%' || <untyped null param> || '%'} resolves to {@code bytea}, breaking the
     * subsequent {@code lower(...)} call ("function lower(bytea) does not exist").
     */
    @Query(
            """
            select i from InventoryItem i
            where i.branchId = :branchId
              and i.deletedAt is null
              and (:categoryId is null or i.categoryId = :categoryId)
              and (:nodeId is null or i.nodeId = :nodeId)
              and (:searchPattern is null
                   or lower(i.name) like :searchPattern
                   or lower(i.sku) like :searchPattern
                   or lower(i.barcode) like :searchPattern)
            """)
    Page<InventoryItem> search(
            @Param("branchId") UUID branchId,
            @Param("categoryId") UUID categoryId,
            @Param("nodeId") UUID nodeId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);
}
