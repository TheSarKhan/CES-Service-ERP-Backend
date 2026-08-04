package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventorySettings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySettingsRepository extends JpaRepository<InventorySettings, UUID> {

    Optional<InventorySettings> findByBranchIdAndDeletedAtIsNull(UUID branchId);

    /** Every configured branch — the scheduled digest walks these without a request context. */
    List<InventorySettings> findByDeletedAtIsNull();
}
