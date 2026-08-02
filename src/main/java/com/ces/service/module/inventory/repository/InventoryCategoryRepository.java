package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, UUID> {

    Optional<InventoryCategory> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    List<InventoryCategory> findByBranchIdAndDeletedAtIsNullOrderByNameAsc(UUID branchId);

    boolean existsByBranchIdAndNameAndDeletedAtIsNull(UUID branchId, String name);

    boolean existsByBranchIdAndNameAndDeletedAtIsNullAndIdNot(UUID branchId, String name, UUID excludeId);
}
