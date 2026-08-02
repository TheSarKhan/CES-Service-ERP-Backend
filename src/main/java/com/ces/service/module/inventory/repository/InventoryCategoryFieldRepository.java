package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryCategoryField;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCategoryFieldRepository extends JpaRepository<InventoryCategoryField, UUID> {

    List<InventoryCategoryField> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    List<InventoryCategoryField> findByCategoryIdInOrderBySortOrderAsc(List<UUID> categoryIds);

    Optional<InventoryCategoryField> findByIdAndCategoryId(UUID id, UUID categoryId);

    boolean existsByCategoryIdAndFieldKey(UUID categoryId, String fieldKey);

    boolean existsByCategoryIdAndFieldKeyAndIdNot(UUID categoryId, String fieldKey, UUID excludeId);
}
