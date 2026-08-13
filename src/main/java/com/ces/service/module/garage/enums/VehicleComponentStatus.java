package com.ces.service.module.garage.enums;

/**
 * A component's lifecycle, recorded on the row itself rather than in a separate history table —
 * the same pattern {@code InventoryItemUnit} uses. A replaced component does not disappear: it
 * flips to REMOVED and a new row opens ACTIVE, so the full swap history is just every row for the
 * vehicle, active and removed alike.
 */
public enum VehicleComponentStatus {
    ACTIVE,
    REMOVED
}
