package com.ces.service.module.garage.enums;

/**
 * Who a piece of equipment belongs to.
 *
 * <p>The SRS originally split this three ways (COMPANY/INVESTOR/CUSTOMER); the product brief
 * collapsed it to two, deferring the investor/customer distinction to the future Müştərilər
 * module — this level only needs to know whether {@code Vehicle.ownerId} means anything.
 */
public enum GarageType {
    COMPANY,
    CUSTOMER
}
