package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.VehicleDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    Optional<VehicleDocument> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);

    List<VehicleDocument> findByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID vehicleId);
}
