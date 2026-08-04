package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryTransfer;
import com.ces.service.module.inventory.enums.TransferStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, UUID> {

    Optional<InventoryTransfer> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    /** See {@code InventoryItemUnitRepository} for why the status variants are split. */
    @Query("select t from InventoryTransfer t where t.branchId = :branchId and t.deletedAt is null")
    Page<InventoryTransfer> findAllTransfers(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("select t from InventoryTransfer t "
            + "where t.branchId = :branchId and t.deletedAt is null and t.status = :status")
    Page<InventoryTransfer> findByStatus(
            @Param("branchId") UUID branchId,
            @Param("status") TransferStatus status,
            Pageable pageable);

    @Query("select count(t) from InventoryTransfer t "
            + "where t.branchId = :branchId and t.deletedAt is null and t.status = 'IN_TRANSIT'")
    long countInTransit(@Param("branchId") UUID branchId);
}
