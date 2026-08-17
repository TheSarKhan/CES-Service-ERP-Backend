package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private BigDecimal marketValue;
    private BigDecimal depreciationPercent;
    private List<String> safetyEquipment;
    private List<String> mandatoryDocuments;
    private UUID primaryPhotoId;
    private String primaryPhotoUrl;
    private List<VehicleParameterItem> parameters;
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
                .purchaseDate(v.getPurchaseDate())
                .purchasePrice(v.getPurchasePrice())
                .marketValue(v.getMarketValue())
                .depreciationPercent(v.getDepreciationPercent())
                .safetyEquipment(v.getSafetyEquipment())
                .mandatoryDocuments(v.getMandatoryDocuments())
                .primaryPhotoId(v.getPrimaryPhotoId())
                .primaryPhotoUrl(v.getPrimaryPhotoUrl())
                .parameters(parseParameters(v.getParameters()))
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private static List<VehicleParameterItem> parseParameters(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<VehicleParameterItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
