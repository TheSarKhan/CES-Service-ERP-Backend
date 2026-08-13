package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.VehicleComponent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleComponentRepository extends JpaRepository<VehicleComponent, UUID> {

    Optional<VehicleComponent> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);

    /** Full history, active and removed alike — newest install first. */
    List<VehicleComponent> findByVehicleIdAndDeletedAtIsNullOrderByInstalledAtDesc(UUID vehicleId);
}
