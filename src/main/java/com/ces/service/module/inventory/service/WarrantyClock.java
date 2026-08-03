package com.ces.service.module.inventory.service;

import com.ces.service.module.inventory.enums.WarrantyStatus;
import java.time.LocalDate;

/**
 * Single source of truth for "is this warranty still good, and for how much longer".
 *
 * <p>Items and serialized units both carry warranty windows and must agree on when one counts as
 * expiring — otherwise a product could read ACTIVE in one screen and EXPIRING_SOON in another.
 */
public final class WarrantyClock {

    /** How far ahead an end date counts as "bitmək üzrə". */
    public static final int EXPIRING_SOON_DAYS = 30;

    private WarrantyClock() {
    }

    public static WarrantyStatus statusOf(LocalDate warrantyEndDate) {
        return statusOf(warrantyEndDate, LocalDate.now());
    }

    /** Overload with an explicit "today" so the rule stays testable and clock-independent. */
    public static WarrantyStatus statusOf(LocalDate warrantyEndDate, LocalDate today) {
        if (warrantyEndDate == null) {
            return WarrantyStatus.NONE;
        }
        if (warrantyEndDate.isBefore(today)) {
            return WarrantyStatus.EXPIRED;
        }
        if (!warrantyEndDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
            return WarrantyStatus.EXPIRING_SOON;
        }
        return WarrantyStatus.ACTIVE;
    }

    /** The window a {@code warrantyMonths} duration produces from a start date. */
    public static LocalDate endDateFrom(LocalDate startDate, Integer warrantyMonths) {
        if (startDate == null || warrantyMonths == null || warrantyMonths <= 0) {
            return null;
        }
        return startDate.plusMonths(warrantyMonths);
    }
}
