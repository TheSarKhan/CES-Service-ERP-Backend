package com.ces.service.infrastructure.erp.dto;

import java.util.UUID;

/**
 * Customer payload returned by CES ERP (SRS §M13).
 *
 * @param erpId    ERP-side customer UUID
 * @param fullName customer name
 * @param voen     tax id (VÖEN)
 * @param phone    contact phone
 * @param email    contact email
 * @param address  postal address
 */
public record ErpCustomerResponse(
        UUID erpId,
        String fullName,
        String voen,
        String phone,
        String email,
        String address
) {
}
