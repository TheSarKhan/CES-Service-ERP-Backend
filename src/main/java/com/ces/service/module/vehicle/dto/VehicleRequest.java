package com.ces.service.module.vehicle.dto;

import com.ces.service.module.vehicle.enums.GarageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Create / update payload for a vehicle (M03.2). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotNull
    private GarageType garageType;

    /** Required for INVESTOR / CUSTOMER garage types (M03.3); may be null for COMPANY. */
    private UUID ownerId;

    @NotBlank
    @Size(max = 100)
    private String make;

    @NotBlank
    @Size(max = 100)
    private String model;

    private Short year;

    @Size(max = 100)
    private String chassisNumber;

    @Size(max = 100)
    private String serialNumber;

    @Size(max = 50)
    private String plateNumber;

    @Size(max = 100)
    private String vehicleType;

    private String currentLocation;

    private String notes;
}
