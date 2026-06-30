package com.ces.service.module.vehicle.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.vehicle.enums.GarageType;
import com.ces.service.module.vehicle.enums.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Vehicle / equipment record (M03 Garage). Extends {@link BaseEntity} (branch_id + audit + soft
 * delete). {@code chassis_number} is globally unique. {@code current_engine_hours} /
 * {@code last_engine_hours_at} are denormalized from the Engine Hours module (M08).
 */
@Entity
@Table(name = "vehicles", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Vehicle extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "garage_type", nullable = false, length = 50)
    private GarageType garageType;

    /** investors.id | customers.id depending on garage_type; null allowed for COMPANY. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "make", nullable = false, length = 100)
    private String make;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "year")
    private Short year;

    @Column(name = "chassis_number", length = 100, unique = true)
    private String chassisNumber;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "plate_number", length = 50)
    private String plateNumber;

    @Column(name = "vehicle_type", length = 100)
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "current_location", columnDefinition = "text")
    private String currentLocation;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Latest engine-hours reading (NUMERIC(10,1)); maintained by the Engine Hours module. */
    @Column(name = "current_engine_hours", precision = 10, scale = 1)
    private BigDecimal currentEngineHours;

    @Column(name = "last_engine_hours_at")
    private Instant lastEngineHoursAt;
}
