package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.WarrantyExtension;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarrantyExtensionRepository extends JpaRepository<WarrantyExtension, UUID> {

    List<WarrantyExtension> findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            WarrantyTargetType targetType, UUID targetId);
}
