package com.ces.service.module.enginehours.repository;

import com.ces.service.module.enginehours.entity.MeterReading;
import com.ces.service.module.enginehours.enums.MeterType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

    List<MeterReading> findByVehicleIdAndDeletedAtIsNullOrderByRecordedAtDescCreatedAtDesc(UUID vehicleId);

    Optional<MeterReading> findFirstByVehicleIdAndMeterTypeAndDeletedAtIsNullOrderByRecordedAtDescCreatedAtDesc(
            UUID vehicleId, MeterType meterType);
}
