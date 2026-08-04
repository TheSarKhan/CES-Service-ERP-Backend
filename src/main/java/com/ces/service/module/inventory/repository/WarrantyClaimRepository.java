package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.WarrantyClaim;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import java.util.Collection;
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
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, UUID> {

    Optional<WarrantyClaim> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    List<WarrantyClaim> findByBranchIdAndTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID branchId, WarrantyTargetType targetType, UUID targetId);

    /**
     * Every claim touching the given targets, newest first — the search panel stamps each result
     * row with its latest claim, and doing that in one query beats one round trip per row.
     */
    @Query("select c from WarrantyClaim c "
            + "where c.branchId = :branchId and c.deletedAt is null and c.targetId in :targetIds "
            + "order by c.createdAt desc")
    List<WarrantyClaim> findByTargets(
            @Param("branchId") UUID branchId, @Param("targetIds") Collection<UUID> targetIds);

    /** See {@code InventoryItemUnitRepository} for why the status variants are split out. */
    @Query("select c from WarrantyClaim c "
            + "where c.branchId = :branchId and c.deletedAt is null "
            + "and (:searchPattern is null "
            + "     or lower(c.targetLabel) like :searchPattern "
            + "     or lower(c.supplier) like :searchPattern "
            + "     or lower(c.claimNumber) like :searchPattern)")
    Page<WarrantyClaim> searchAllStatuses(
            @Param("branchId") UUID branchId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select c from WarrantyClaim c "
            + "where c.branchId = :branchId and c.deletedAt is null and c.status = :status "
            + "and (:searchPattern is null "
            + "     or lower(c.targetLabel) like :searchPattern "
            + "     or lower(c.supplier) like :searchPattern "
            + "     or lower(c.claimNumber) like :searchPattern)")
    Page<WarrantyClaim> searchByStatus(
            @Param("branchId") UUID branchId,
            @Param("status") WarrantyClaimStatus status,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select count(c) from WarrantyClaim c "
            + "where c.branchId = :branchId and c.deletedAt is null and c.status = 'SUBMITTED'")
    long countOpen(@Param("branchId") UUID branchId);
}
