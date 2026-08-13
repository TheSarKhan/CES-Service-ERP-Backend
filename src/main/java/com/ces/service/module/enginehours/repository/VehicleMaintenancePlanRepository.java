package com.ces.service.module.enginehours.repository;

import com.ces.service.module.enginehours.entity.VehicleMaintenancePlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleMaintenancePlanRepository extends JpaRepository<VehicleMaintenancePlan, UUID> {

    List<VehicleMaintenancePlan> findByVehicleIdAndDeletedAtIsNullOrderByMaintenanceTypeAsc(UUID vehicleId);

    Optional<VehicleMaintenancePlan> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
}
