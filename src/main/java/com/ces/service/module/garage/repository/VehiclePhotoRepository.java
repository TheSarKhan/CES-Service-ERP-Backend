package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.VehiclePhoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, UUID> {

    Optional<VehiclePhoto> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);

    List<VehiclePhoto> findByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID vehicleId);
}
