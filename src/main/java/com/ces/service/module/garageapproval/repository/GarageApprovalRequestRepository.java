package com.ces.service.module.garageapproval.repository;

import com.ces.service.module.approval.entity.ApprovalStatus;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalRequest;
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
public interface GarageApprovalRequestRepository extends JpaRepository<GarageApprovalRequest, UUID> {

    Optional<GarageApprovalRequest> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    /** The lock: a target with a pending request cannot be touched again until it's decided. */
    Optional<GarageApprovalRequest> findFirstByEntityTypeAndEntityIdAndStatusAndDeletedAtIsNull(
            GarageApprovalEntityType entityType, UUID entityId, ApprovalStatus status);

    @Query("select r.entityId from GarageApprovalRequest r "
            + "where r.branchId = :branchId and r.entityType = :entityType "
            + "and r.entityId in :entityIds and r.status = 'PENDING' and r.deletedAt is null")
    List<UUID> findPendingEntityIds(
            @Param("branchId") UUID branchId,
            @Param("entityType") GarageApprovalEntityType entityType,
            @Param("entityIds") List<UUID> entityIds);

    @Query("select r from GarageApprovalRequest r "
            + "where r.branchId = :branchId and r.deletedAt is null "
            + "and (:status is null or r.status = :status)")
    Page<GarageApprovalRequest> search(
            @Param("branchId") UUID branchId,
            @Param("status") ApprovalStatus status,
            Pageable pageable);

    long countByBranchIdAndStatusAndDeletedAtIsNull(UUID branchId, ApprovalStatus status);
}
