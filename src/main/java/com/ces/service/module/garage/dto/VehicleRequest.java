package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.enums.GarageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create/update payload. Only {@code name}, {@code vehicleType}, {@code make}, {@code model} are
 * required — the brief flags identification numbers as business-decided-optional: a lot of real
 * equipment (excavators, generators) never carries a plate number, and forcing one blocks
 * registration for no reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private GarageType garageType;

    /** {@code customers.id} — required when garageType is CUSTOMER, validated server-side. */
    private UUID ownerId;

    @NotBlank
    @Size(max = 100)
    private String vehicleType;

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

    /** Free text, validated against garage_config_values(STATUS) — null on create means "Aktiv". */
    private String status;

    @Size(max = 255)
    private String currentLocation;

    private String notes;

    /**
     * Null on create means "copy the equipment type's default"; present means an explicit
     * per-vehicle override, per the brief's "konkret texnika səviyyəsində dəyişdirilə bilməli".
     */
    private Boolean usesEngineHours;

    private Boolean usesKm;

    /** Starting reading, applied once at creation only — later values come from real readings. */
    private BigDecimal initialMeterValue;

    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    private BigDecimal marketValue;

    private BigDecimal depreciationPercent;

    /** Each a {@code garage_config_values(SAFETY_EQUIPMENT)} value. Null on create means none. */
    private List<String> safetyEquipment;

    /** Each a {@code garage_config_values(MANDATORY_DOCUMENT)} value. Null on create means none. */
    private List<String> mandatoryDocuments;

    /** Free-form "ad: dəyər" technical parameter rows. Null on create means none. */
    private List<VehicleParameterItem> parameters;
}
