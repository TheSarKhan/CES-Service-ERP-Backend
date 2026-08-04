package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.Stocktake;
import com.ces.service.module.inventory.enums.StocktakeStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeRepository extends JpaRepository<Stocktake, UUID> {

    Optional<Stocktake> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    /** A folder can only have one sheet running at a time. */
    @Query("select s from Stocktake s where s.branchId = :branchId and s.nodeId = :nodeId "
            + "and s.deletedAt is null and s.status in ('OPEN', 'PENDING_APPROVAL')")
    Optional<Stocktake> findActiveForNode(
            @Param("branchId") UUID branchId, @Param("nodeId") UUID nodeId);

    @Query("select s from Stocktake s where s.branchId = :branchId and s.deletedAt is null")
    Page<Stocktake> findAllStocktakes(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("select s from Stocktake s "
            + "where s.branchId = :branchId and s.deletedAt is null and s.status = :status")
    Page<Stocktake> findByStatus(
            @Param("branchId") UUID branchId,
            @Param("status") StocktakeStatus status,
            Pageable pageable);

    Optional<Stocktake> findByApprovalRequestIdAndDeletedAtIsNull(UUID approvalRequestId);
}
