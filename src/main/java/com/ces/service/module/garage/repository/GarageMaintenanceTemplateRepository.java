package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.GarageMaintenanceTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarageMaintenanceTemplateRepository extends JpaRepository<GarageMaintenanceTemplate, UUID> {

    Optional<GarageMaintenanceTemplate> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    List<GarageMaintenanceTemplate> findByBranchIdAndDeletedAtIsNullOrderByEquipmentTypeAscNameAsc(UUID branchId);

    List<GarageMaintenanceTemplate> findByBranchIdAndEquipmentTypeAndIsActiveTrueAndDeletedAtIsNull(
            UUID branchId, String equipmentType);
}
