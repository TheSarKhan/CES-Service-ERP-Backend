package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryItemUnit;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
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
public interface InventoryItemUnitRepository extends JpaRepository<InventoryItemUnit, UUID> {

    Optional<InventoryItemUnit> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    List<InventoryItemUnit> findByItemIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID itemId);

    long countByItemIdAndStatusAndDeletedAtIsNull(UUID itemId, InventoryUnitStatus status);

    boolean existsByBranchIdAndSerialNumberAndDeletedAtIsNull(UUID branchId, String serialNumber);

    Optional<InventoryItemUnit> findByBranchIdAndQrCodeAndDeletedAtIsNull(UUID branchId, String qrCode);

    Optional<InventoryItemUnit> findByBranchIdAndBarcodeAndDeletedAtIsNull(UUID branchId, String barcode);

    Optional<InventoryItemUnit> findByBranchIdAndSerialNumberAndDeletedAtIsNull(UUID branchId, String serialNumber);

    /**
     * Warranty windows closing inside the given range. Disposed units are ignored — a warranty on
     * something already written off isn't an action anyone needs prompting about.
     */
    @Query("select count(u) from InventoryItemUnit u "
            + "where u.branchId = :branchId and u.deletedAt is null "
            + "and u.status <> 'DISPOSED' and u.warrantyEndDate is not null "
            + "and u.warrantyEndDate between :from and :to")
    long countUnitsWithWarrantyEndBetween(
            @Param("branchId") UUID branchId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("select count(u) from InventoryItemUnit u "
            + "where u.branchId = :branchId and u.deletedAt is null "
            + "and u.status <> 'DISPOSED' and u.warrantyEndDate is not null "
            + "and u.warrantyEndDate < :today")
    long countUnitsWithWarrantyExpired(
            @Param("branchId") UUID branchId, @Param("today") LocalDate today);

    /**
     * Warranty search — joins to {@code InventoryItem} by id (no mapped JPA association exists
     * between the two, so this uses an explicit theta-join). Matches by serial number, item name,
     * or SKU (case-insensitive) via {@code searchPattern}, pre-built by the caller as
     * {@code "%" + search.toLowerCase() + "%"} (or null) — see
     * {@code InventoryItemRepository.search} javadoc for why this isn't done via SQL
     * {@code concat()}.
     *
     * <p>Split into {@code searchAllStatuses}/{@code searchByStatus} rather than one method with an
     * {@code (:status is null or ...)} clause: PgJDBC's extended protocol cannot infer a type for a
     * bare {@code ? is null} check on this enum-mapped column ("could not determine data type of
     * parameter") — passing null for {@code itemId} (UUID) is fine, only the enum hits this.
     */
    @Query(
            """
            select u from InventoryItemUnit u, InventoryItem i
            where i.id = u.itemId
              and u.branchId = :branchId
              and u.deletedAt is null
              and (:itemId is null or u.itemId = :itemId)
              and (:searchPattern is null
                   or lower(u.serialNumber) like :searchPattern
                   or lower(i.name) like :searchPattern
                   or lower(i.sku) like :searchPattern)
            """)
    Page<InventoryItemUnit> searchAllStatuses(
            @Param("branchId") UUID branchId,
            @Param("itemId") UUID itemId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query(
            """
            select u from InventoryItemUnit u, InventoryItem i
            where i.id = u.itemId
              and u.branchId = :branchId
              and u.deletedAt is null
              and u.status = :status
              and (:itemId is null or u.itemId = :itemId)
              and (:searchPattern is null
                   or lower(u.serialNumber) like :searchPattern
                   or lower(i.name) like :searchPattern
                   or lower(i.sku) like :searchPattern)
            """)
    Page<InventoryItemUnit> searchByStatus(
            @Param("branchId") UUID branchId,
            @Param("itemId") UUID itemId,
            @Param("status") InventoryUnitStatus status,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);
}
