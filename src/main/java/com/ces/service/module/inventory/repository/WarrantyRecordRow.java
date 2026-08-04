package com.ces.service.module.inventory.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row of the unified warranty search — either a serialized unit or a non-serialized product.
 *
 * <p>Getter names match the quoted column aliases in {@link WarrantyRecordRepository} exactly, so
 * the mapping doesn't depend on Spring Data's snake_case fallback.
 */
public interface WarrantyRecordRow {

    /** Unit id for a UNIT row, item id for an ITEM row. */
    UUID getRecordId();

    /** {@code "UNIT"} or {@code "ITEM"}. */
    String getRecordType();

    UUID getItemId();

    String getItemName();

    String getItemSku();

    /** Null on ITEM rows. */
    String getSerialNumber();

    /** {@code InventoryUnitStatus} name; null on ITEM rows. */
    String getUnitStatus();

    UUID getNodeId();

    String getBarcode();

    String getQrCode();

    LocalDate getWarrantyStartDate();

    LocalDate getWarrantyEndDate();

    String getSupplier();

    /** Stock on hand; null on UNIT rows, where the unit *is* the quantity. */
    BigDecimal getQuantity();

    String getUnit();
}
