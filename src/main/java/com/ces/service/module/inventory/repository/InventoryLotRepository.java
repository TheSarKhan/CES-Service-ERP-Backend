package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryLot;
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
public interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {

    Optional<InventoryLot> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    Optional<InventoryLot> findByItemIdAndNodeIdAndLotNumberAndDeletedAtIsNull(
            UUID itemId, UUID nodeId, String lotNumber);

    List<InventoryLot> findByItemIdAndDeletedAtIsNull(UUID itemId);

    /**
     * Batches at a folder in FEFO order — soonest expiry first, undated last.
     *
     * <p>Empty batches are excluded: a lot with nothing left is history, and offering it as the
     * suggested pick would be offering nothing.
     */
    @Query("select l from InventoryLot l "
            + "where l.itemId = :itemId and l.nodeId = :nodeId "
            + "and l.deletedAt is null and l.quantity > 0 "
            + "order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate asc, l.receivedDate asc")
    List<InventoryLot> findFefoOrder(@Param("itemId") UUID itemId, @Param("nodeId") UUID nodeId);

    /** Batches running out of time, soonest first. */
    @Query("select l from InventoryLot l "
            + "where l.branchId = :branchId and l.deletedAt is null and l.quantity > 0 "
            + "and l.expiryDate is not null and l.expiryDate <= :horizon "
            + "order by l.expiryDate asc")
    Page<InventoryLot> findExpiring(
            @Param("branchId") UUID branchId, @Param("horizon") LocalDate horizon, Pageable pageable);

    @Query("select count(l) from InventoryLot l "
            + "where l.branchId = :branchId and l.deletedAt is null and l.quantity > 0 "
            + "and l.expiryDate is not null and l.expiryDate < :today")
    long countExpired(@Param("branchId") UUID branchId, @Param("today") LocalDate today);

    @Query("select count(l) from InventoryLot l "
            + "where l.branchId = :branchId and l.deletedAt is null and l.quantity > 0 "
            + "and l.expiryDate is not null and l.expiryDate >= :today and l.expiryDate <= :horizon")
    long countExpiringSoon(
            @Param("branchId") UUID branchId,
            @Param("today") LocalDate today,
            @Param("horizon") LocalDate horizon);
}
