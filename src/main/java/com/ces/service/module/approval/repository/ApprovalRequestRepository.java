package com.ces.service.module.approval.repository;

import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.entity.ApprovalStatus;
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
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    Optional<ApprovalRequest> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    /** The lock: a target with a pending request cannot be touched again until it's decided. */
    Optional<ApprovalRequest> findFirstByEntityTypeAndEntityIdAndStatusAndDeletedAtIsNull(
            ApprovalEntityType entityType, UUID entityId, ApprovalStatus status);

    /** Pending-request lookup for a batch of entities — powers the "kilidli" badge in list views. */
    @Query("select r.entityId from ApprovalRequest r "
            + "where r.branchId = :branchId and r.entityType = :entityType "
            + "and r.entityId in :entityIds and r.status = 'PENDING' and r.deletedAt is null")
    List<UUID> findPendingEntityIds(
            @Param("branchId") UUID branchId,
            @Param("entityType") ApprovalEntityType entityType,
            @Param("entityIds") List<UUID> entityIds);

    @Query("select r from ApprovalRequest r "
            + "where r.branchId = :branchId and r.deletedAt is null "
            + "and (:status is null or r.status = :status)")
    Page<ApprovalRequest> search(
            @Param("branchId") UUID branchId,
            @Param("status") ApprovalStatus status,
            Pageable pageable);

    long countByBranchIdAndStatusAndDeletedAtIsNull(UUID branchId, ApprovalStatus status);
}
