package com.ces.service.module.vehicle.repository;

import com.ces.service.module.vehicle.entity.VehicleDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Branch-scoped repository for {@code vehicle_documents}. */
@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    List<VehicleDocument> findByVehicleIdAndBranchIdAndDeletedAtIsNull(UUID vehicleId, UUID branchId);

    Optional<VehicleDocument> findByIdAndVehicleIdAndBranchIdAndDeletedAtIsNull(
            UUID id, UUID vehicleId, UUID branchId);
}
