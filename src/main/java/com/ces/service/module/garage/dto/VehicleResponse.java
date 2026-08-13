package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private UUID id;
    private UUID branchId;
    private String code;
    private String name;
    private GarageType garageType;
    private UUID ownerId;
    private String ownerName;
    private String vehicleType;
    private String make;
    private String model;
    private Short year;
    private String chassisNumber;
    private String serialNumber;
    private String plateNumber;
    private String status;
    private String currentLocation;
    private String notes;
    private Boolean usesEngineHours;
    private Boolean usesKm;
    private BigDecimal currentEngineHours;
    private Instant lastEngineHoursAt;
    private BigDecimal currentKm;
    private Instant lastKmAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static VehicleResponse from(Vehicle v) {
        return from(v, null);
    }

    public static VehicleResponse from(Vehicle v, String ownerName) {
        return VehicleResponse.builder()
                .id(v.getId())
                .branchId(v.getBranchId())
                .code(v.getCode())
                .name(v.getName())
                .garageType(v.getGarageType())
                .ownerId(v.getOwnerId())
                .ownerName(ownerName)
                .vehicleType(v.getVehicleType())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .chassisNumber(v.getChassisNumber())
                .serialNumber(v.getSerialNumber())
                .plateNumber(v.getPlateNumber())
                .status(v.getStatus())
                .currentLocation(v.getCurrentLocation())
                .notes(v.getNotes())
                .usesEngineHours(v.getUsesEngineHours())
                .usesKm(v.getUsesKm())
                .currentEngineHours(v.getCurrentEngineHours())
                .lastEngineHoursAt(v.getLastEngineHoursAt())
                .currentKm(v.getCurrentKm())
                .lastKmAt(v.getLastKmAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
