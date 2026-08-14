package com.ces.service.module.garageapproval.entity;

/** The deferred operation, scoped to what Qaraj/Motosaat actually defer. */
public enum GarageApprovalOperation {
    UPDATE,
    DELETE,
    /** A meter reading lower than the vehicle's current value — see {@code MeterReadingService}. */
    METER_ROLLOVER
}
