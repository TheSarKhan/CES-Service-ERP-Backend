package com.ces.service.module.vehicle.enums;

/** Operational status of a vehicle (M03). Stored as VARCHAR(50) + CHECK; defaults to ACTIVE. */
public enum VehicleStatus {
    ACTIVE,
    IN_SERVICE,
    INACTIVE
}
