package com.ces.service.infrastructure.erp.dto;

import java.util.UUID;

/**
 * Customer create/update payload sent to CES ERP (SRS §M13).
 *
 * @param branchId source branch UUID (CES Service)
 * @param fullName customer name
 * @param voen     tax id (VÖEN)
 * @param phone    contact phone
 * @param email    contact email
 * @param address  postal address
 */
public record ErpCustomerRequest(
        UUID branchId,
        String fullName,
        String voen,
        String phone,
        String email,
        String address
) {
}
