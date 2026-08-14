package com.ces.service.module.garageapproval.entity;

/** Kind of record a {@link GarageApprovalRequest} targets — Qaraj/Motosaat's own queue, separate from Anbar's. */
public enum GarageApprovalEntityType {
    /** A piece of equipment (Qaraj) — see {@code VehicleApprovalExecutor}. */
    VEHICLE,
    /** A vehicle's meter (Motosaat) — locks by vehicle id. See {@code MeterReadingApprovalExecutor}. */
    METER_READING
}
