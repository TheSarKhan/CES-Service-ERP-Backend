package com.ces.service.module.enginehours.repository;

import com.ces.service.module.enginehours.entity.MeterReading;
import com.ces.service.module.enginehours.enums.MeterType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

    Page<MeterReading> findByVehicleIdAndDeletedAtIsNull(UUID vehicleId, Pageable pageable);

    Optional<MeterReading> findFirstByVehicleIdAndMeterTypeAndDeletedAtIsNullOrderByRecordedAtDescCreatedAtDesc(
            UUID vehicleId, MeterType meterType);
}
