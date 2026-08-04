package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryStock;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {

    List<InventoryStock> findByItemIdAndDeletedAtIsNull(UUID itemId);

    Optional<InventoryStock> findByItemIdAndNodeIdAndDeletedAtIsNull(UUID itemId, UUID nodeId);

    boolean existsByNodeIdAndDeletedAtIsNull(UUID nodeId);

    /**
     * Same row, but locked for the duration of the transaction.
     *
     * <p>Two people receiving goods into the same shelf at the same moment would otherwise both
     * read the old quantity and the second write would erase the first. Stock is the one place in
     * this module where that actually loses money, so the read is serialised.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InventoryStock s "
            + "where s.itemId = :itemId and s.nodeId = :nodeId and s.deletedAt is null")
    Optional<InventoryStock> findForUpdate(@Param("itemId") UUID itemId, @Param("nodeId") UUID nodeId);

    /** Total across every location — the number that answers "how many do we have". */
    @Query("select coalesce(sum(s.quantity), 0) from InventoryStock s "
            + "where s.itemId = :itemId and s.deletedAt is null")
    BigDecimal totalForItem(@Param("itemId") UUID itemId);

    /** Totals for a page of products in one query, rather than one round trip per row. */
    @Query("select s.itemId, coalesce(sum(s.quantity), 0) from InventoryStock s "
            + "where s.itemId in :itemIds and s.deletedAt is null group by s.itemId")
    List<Object[]> totalsForItems(@Param("itemIds") Collection<UUID> itemIds);

    /** Every location holding a page of products, for the "where is it" column. */
    @Query("select s from InventoryStock s where s.itemId in :itemIds and s.deletedAt is null")
    List<InventoryStock> findByItemIds(@Param("itemIds") Collection<UUID> itemIds);

    /** Distinct categories actually present at a node — powers the per-category sections. */
    @Query("select distinct i.categoryId from InventoryStock s, InventoryItem i "
            + "where i.id = s.itemId and s.branchId = :branchId and s.nodeId = :nodeId "
            + "and s.deletedAt is null and i.deletedAt is null")
    List<UUID> findDistinctCategoryIdsAtNode(
            @Param("branchId") UUID branchId, @Param("nodeId") UUID nodeId);
}
