package com.ces.service.module.vehicle.dto;

import com.ces.service.module.vehicle.enums.GarageType;
import com.ces.service.module.vehicle.enums.VehicleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vehicle view (list + detail). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private UUID id;
    private UUID branchId;
    private GarageType garageType;
    private UUID ownerId;
    private String make;
    private String model;
    private Short year;
    private String chassisNumber;
    private String serialNumber;
    private String plateNumber;
    private String vehicleType;
    private VehicleStatus status;
    private String currentLocation;
    private String notes;
    private BigDecimal currentEngineHours;
    private Instant lastEngineHoursAt;
    private Instant createdAt;
    private Instant updatedAt;
}
