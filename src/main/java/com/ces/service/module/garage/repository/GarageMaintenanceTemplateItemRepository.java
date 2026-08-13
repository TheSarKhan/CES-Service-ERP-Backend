package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.GarageMaintenanceTemplateItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarageMaintenanceTemplateItemRepository extends JpaRepository<GarageMaintenanceTemplateItem, UUID> {

    List<GarageMaintenanceTemplateItem> findByTemplateIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
