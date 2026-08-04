package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryTransferLine;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransferLineRepository extends JpaRepository<InventoryTransferLine, UUID> {

    List<InventoryTransferLine> findByTransferIdAndDeletedAtIsNull(UUID transferId);

    List<InventoryTransferLine> findByTransferIdInAndDeletedAtIsNull(Collection<UUID> transferIds);

    /**
     * How much of a product is on a trolley right now — stock that has left one shelf and not yet
     * arrived at another. Shown on the product card so the total and the shelves add up.
     */
    @Query("select coalesce(sum(l.quantity), 0) from InventoryTransferLine l, InventoryTransfer t "
            + "where t.id = l.transferId and l.itemId = :itemId "
            + "and l.deletedAt is null and t.deletedAt is null and t.status = 'IN_TRANSIT'")
    BigDecimal inTransitQuantity(@Param("itemId") UUID itemId);
}
