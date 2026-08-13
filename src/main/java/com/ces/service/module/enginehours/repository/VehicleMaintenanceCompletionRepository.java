package com.ces.service.module.enginehours.repository;

import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleMaintenanceCompletionRepository extends JpaRepository<VehicleMaintenanceCompletion, UUID> {

    List<VehicleMaintenanceCompletion> findByVehicleIdAndDeletedAtIsNullOrderByCompletedAtDesc(UUID vehicleId);

    List<VehicleMaintenanceCompletion> findByPlanIdAndDeletedAtIsNullOrderByCompletedAtDesc(UUID planId);
}
