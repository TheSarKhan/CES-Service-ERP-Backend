package com.ces.service.module.garage.enums;

/**
 * Which dropdown list a {@code GarageConfigValue} row belongs to.
 *
 * <p>One table serves all of these deliberately: Qaraj never needs a per-type custom FIELD
 * schema the way Inventory categories do, only manageable dropdown VALUES. A full EAV system
 * here would be building for a requirement that was never asked for.
 */
public enum GarageConfigListType {
    EQUIPMENT_TYPE,
    BRAND,
    MODEL,
    STATUS,
    LOCATION,
    DOC_TYPE,
    PHOTO_CATEGORY,
    COMPONENT_TYPE,
    MAINTENANCE_TYPE,
    METER_SOURCE,
    SAFETY_EQUIPMENT,
    MANDATORY_DOCUMENT
}
