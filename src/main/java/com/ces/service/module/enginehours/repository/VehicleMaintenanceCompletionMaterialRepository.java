package com.ces.service.module.enginehours.repository;

import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletionMaterial;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleMaintenanceCompletionMaterialRepository
        extends JpaRepository<VehicleMaintenanceCompletionMaterial, UUID> {

    List<VehicleMaintenanceCompletionMaterial> findByCompletionIdAndDeletedAtIsNull(UUID completionId);

    List<VehicleMaintenanceCompletionMaterial> findByCompletionIdInAndDeletedAtIsNull(List<UUID> completionIds);
}
