package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import com.ces.service.module.inventory.enums.WarrantyRecordType;
import com.ces.service.module.inventory.enums.WarrantyStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filters for the unified warranty search. Bundled into one object rather than passed as eight
 * loose arguments, which is where argument-order mistakes come from.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyRecordSearchCriteria {

    /** Matches serial number, product name, SKU, barcode, QR code or supplier. */
    private String search;

    /** Restrict to serialized units or to whole products; null means both. */
    private WarrantyRecordType recordType;

    private WarrantyStatus warrantyStatus;

    /** Only meaningful for units — setting it excludes product rows entirely. */
    private InventoryUnitStatus unitStatus;

    private String supplier;

    private LocalDate endFrom;

    private LocalDate endTo;

    /**
     * Shorthand for "expiring within N days": expands to {@code endFrom = today},
     * {@code endTo = today + days}. Ignored when an explicit range is given.
     */
    private Integer withinDays;
}
