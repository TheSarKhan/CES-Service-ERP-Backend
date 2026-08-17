package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.GarageSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarageSettingsRepository extends JpaRepository<GarageSettings, UUID> {

    Optional<GarageSettings> findByBranchIdAndDeletedAtIsNull(UUID branchId);
}
