package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryItem;
import java.time.LocalDate;
import java.util.List;
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
     * Non-serialized products whose warranty ends inside the given window. Serialized ones are
     * excluded on purpose: their warranty lives on each unit, so counting the parent would double
     * up with the unit-level query.
     */
    @Query("select count(i) from InventoryItem i "
            + "where i.branchId = :branchId and i.deletedAt is null "
            + "and i.isSerialized = false and i.warrantyEndDate is not null "
            + "and i.warrantyEndDate between :from and :to")
    long countItemsWithWarrantyEndBetween(
            @Param("branchId") UUID branchId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("select count(i) from InventoryItem i "
            + "where i.branchId = :branchId and i.deletedAt is null "
            + "and i.isSerialized = false and i.warrantyEndDate is not null "
            + "and i.warrantyEndDate < :today")
    long countItemsWithWarrantyExpired(
            @Param("branchId") UUID branchId, @Param("today") LocalDate today);

    /**
     * Distinct category ids actually present at a node — lets the frontend know which category
     * sections to render for an unrestricted node (no {@code categoryIds} allow-list) once each
     * section fetches its own paginated item page instead of one bulk fetch to group client-side.
     */
    @Query(
            "select distinct i.categoryId from InventoryItem i "
                    + "where i.branchId = :branchId and i.nodeId = :nodeId and i.deletedAt is null")
    List<UUID> findDistinctCategoryIdsByNodeId(@Param("branchId") UUID branchId, @Param("nodeId") UUID nodeId);

    /**
     * Filtered + searchable listing. Any of the filter params may be null (ignored when null).
     * {@code searchPattern} matches name / SKU / barcode / any dynamic-field value (case-
     * insensitive) — callers must pre-build it as {@code "%" + search.toLowerCase() + "%"} (or
     * pass null). Building the wildcard pattern in Java rather than via SQL {@code concat()}
     * avoids a PgJDBC quirk where {@code '%' || <untyped null param> || '%'} resolves to
     * {@code bytea}, breaking the subsequent {@code lower(...)} call.
     *
     * <p>Native (not JPQL) so {@code attributes} — JSONB, mapped as a raw string — can be cast to
     * text and searched directly; this is what powers the "hər cür dəyərlə tapaq" global product
     * search (categoryId and nodeId both null) as well as the per-node/category listings.
     */
    @Query(
            value = """
            select * from ces_service.inventory_items i
            where i.branch_id = :branchId
              and i.deleted_at is null
              and (:categoryId is null or i.category_id = :categoryId)
              and (:nodeId is null or i.node_id = :nodeId)
              and (:searchPattern is null
                   or i.name ilike :searchPattern
                   or i.sku ilike :searchPattern
                   or i.barcode ilike :searchPattern
                   or i.attributes::text ilike :searchPattern)
            """,
            countQuery = """
            select count(*) from ces_service.inventory_items i
            where i.branch_id = :branchId
              and i.deleted_at is null
              and (:categoryId is null or i.category_id = :categoryId)
              and (:nodeId is null or i.node_id = :nodeId)
              and (:searchPattern is null
                   or i.name ilike :searchPattern
                   or i.sku ilike :searchPattern
                   or i.barcode ilike :searchPattern
                   or i.attributes::text ilike :searchPattern)
            """,
            nativeQuery = true)
    Page<InventoryItem> search(
            @Param("branchId") UUID branchId,
            @Param("categoryId") UUID categoryId,
            @Param("nodeId") UUID nodeId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);
}
