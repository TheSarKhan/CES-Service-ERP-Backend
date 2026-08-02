package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryNodeRepository extends JpaRepository<InventoryNode, UUID> {

    Optional<InventoryNode> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    Optional<InventoryNode> findByBranchIdAndQrCodeAndDeletedAtIsNull(UUID branchId, String qrCode);

    Optional<InventoryNode> findByBranchIdAndBarcodeAndDeletedAtIsNull(UUID branchId, String barcode);

    /** Children of {@code parentId}, or root nodes when {@code parentId} is null. */
    List<InventoryNode> findByBranchIdAndParentIdAndDeletedAtIsNullOrderByNameAsc(UUID branchId, UUID parentId);

    boolean existsByBranchIdAndParentIdAndNameAndDeletedAtIsNull(UUID branchId, UUID parentId, String name);

    boolean existsByBranchIdAndParentIdAndNameAndDeletedAtIsNullAndIdNot(
            UUID branchId, UUID parentId, String name, UUID excludeId);

    boolean existsByParentIdAndDeletedAtIsNull(UUID parentId);

    /** Distinct node ids (from the given candidates) that currently have at least one child. */
    @Query(
            """
            select distinct n.parentId from InventoryNode n
            where n.parentId in :ids and n.deletedAt is null
            """)
    List<UUID> findIdsWithChildren(@Param("ids") List<UUID> ids);
}
