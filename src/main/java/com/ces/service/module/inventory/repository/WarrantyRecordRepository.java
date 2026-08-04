package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.InventoryItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The unified warranty search.
 *
 * <p>Warranty is tracked in two tables — {@code inventory_item_units} for serialized things and
 * {@code inventory_items} for everything bought as a batch under one warranty. A screen that only
 * queried one of them answered "is this still covered?" for half the warehouse, so this UNIONs
 * both into a single result set and filters over the union.
 *
 * <p>Native SQL rather than JPQL because JPQL has no UNION, and paging over two separate queries
 * cannot produce a correct page or total. Every parameter is wrapped in an explicit
 * {@code CAST(... AS ...)}: PgJDBC's extended protocol cannot infer a type for a bare
 * {@code ? IS NULL} check, which is the same limitation documented on
 * {@link InventoryItemUnitRepository}.
 *
 * <p>A product row carries no node: the same product can sit in several folders, so its quantity
 * is the total across them and {@code nodeId} comes back null — the product card lists the places.
 * A unit row still names its folder, because a unit is in exactly one place.
 *
 * <p>Ordering is fixed (soonest expiry first, nulls last) instead of coming from {@code Pageable} —
 * the whole point of the screen is "what lapses next", and letting a caller sort by an arbitrary
 * column of a derived table would need the column names leaked into the API.
 */
public interface WarrantyRecordRepository extends Repository<InventoryItem, UUID> {

    /**
     * Items with neither an end date nor a warranty length are ordinary stock nobody tracks a
     * warranty for; including them would bury the rows that matter under the whole warehouse.
     * Units are never filtered that way — a unit registered without an end date is itself a gap
     * worth seeing.
     */
    String UNION_SOURCE =
            """
            SELECT
                u.id                      AS record_id,
                'UNIT'                    AS record_type,
                i.id                      AS item_id,
                i.name                    AS item_name,
                i.sku                     AS item_sku,
                u.serial_number           AS serial_number,
                CAST(u.status AS varchar)  AS unit_status,
                u.node_id                 AS node_id,
                u.barcode                 AS barcode,
                u.qr_code                 AS qr_code,
                u.warranty_start_date     AS warranty_start_date,
                u.warranty_end_date       AS warranty_end_date,
                i.supplier                AS supplier,
                CAST(NULL AS numeric)     AS quantity,
                i.unit                    AS unit
            FROM ces_service.inventory_item_units u
            JOIN ces_service.inventory_items i
              ON i.id = u.item_id AND i.deleted_at IS NULL
            WHERE u.branch_id = :branchId AND u.deleted_at IS NULL
            UNION ALL
            SELECT
                i.id, 'ITEM', i.id, i.name, i.sku,
                NULL, NULL, NULL, i.barcode, i.qr_code,
                i.warranty_start_date, i.warranty_end_date, i.supplier,
                COALESCE((
                    SELECT SUM(s.quantity) FROM ces_service.inventory_stock s
                    WHERE s.item_id = i.id AND s.deleted_at IS NULL
                ), 0),
                i.unit
            FROM ces_service.inventory_items i
            WHERE i.branch_id = :branchId
              AND i.deleted_at IS NULL
              AND i.is_serialized = false
              AND (i.warranty_end_date IS NOT NULL OR i.warranty_months IS NOT NULL)
            """;

    /**
     * {@code warrantyStatus} mirrors {@code WarrantyClock} exactly — the boundary between
     * EXPIRING_SOON and ACTIVE must be the same rule the badge uses, or a row could be filtered as
     * one thing and labelled as another. {@code soonDate} is {@code today + EXPIRING_SOON_DAYS},
     * passed in so the two never drift.
     */
    String FILTERS =
            """
            WHERE (CAST(:searchPattern AS text) IS NULL
                   OR lower(r.item_name) LIKE CAST(:searchPattern AS text)
                   OR lower(r.item_sku) LIKE CAST(:searchPattern AS text)
                   OR lower(COALESCE(r.serial_number, '')) LIKE CAST(:searchPattern AS text)
                   OR lower(COALESCE(r.barcode, '')) LIKE CAST(:searchPattern AS text)
                   OR lower(COALESCE(r.qr_code, '')) LIKE CAST(:searchPattern AS text)
                   OR lower(COALESCE(r.supplier, '')) LIKE CAST(:searchPattern AS text))
              AND (CAST(:recordType AS text) IS NULL OR r.record_type = CAST(:recordType AS text))
              AND (CAST(:unitStatus AS text) IS NULL OR r.unit_status = CAST(:unitStatus AS text))
              AND (CAST(:supplier AS text) IS NULL
                   OR lower(COALESCE(r.supplier, '')) = lower(CAST(:supplier AS text)))
              AND (CAST(:endFrom AS date) IS NULL OR r.warranty_end_date >= CAST(:endFrom AS date))
              AND (CAST(:endTo AS date) IS NULL OR r.warranty_end_date <= CAST(:endTo AS date))
              AND (CAST(:warrantyStatus AS text) IS NULL
                   OR (CAST(:warrantyStatus AS text) = 'NONE'
                       AND r.warranty_end_date IS NULL)
                   OR (CAST(:warrantyStatus AS text) = 'EXPIRED'
                       AND r.warranty_end_date < CAST(:today AS date))
                   OR (CAST(:warrantyStatus AS text) = 'EXPIRING_SOON'
                       AND r.warranty_end_date >= CAST(:today AS date)
                       AND r.warranty_end_date <= CAST(:soonDate AS date))
                   OR (CAST(:warrantyStatus AS text) = 'ACTIVE'
                       AND r.warranty_end_date > CAST(:soonDate AS date)))
            """;

    @Query(
            nativeQuery = true,
            value =
                    "SELECT r.record_id AS \"recordId\", r.record_type AS \"recordType\","
                            + " r.item_id AS \"itemId\", r.item_name AS \"itemName\","
                            + " r.item_sku AS \"itemSku\", r.serial_number AS \"serialNumber\","
                            + " r.unit_status AS \"unitStatus\", r.node_id AS \"nodeId\","
                            + " r.barcode AS \"barcode\", r.qr_code AS \"qrCode\","
                            + " r.warranty_start_date AS \"warrantyStartDate\","
                            + " r.warranty_end_date AS \"warrantyEndDate\","
                            + " r.supplier AS \"supplier\", r.quantity AS \"quantity\","
                            + " r.unit AS \"unit\""
                            + " FROM (" + UNION_SOURCE + ") r "
                            + FILTERS
                            + " ORDER BY r.warranty_end_date ASC NULLS LAST, r.item_name ASC,"
                            + " r.serial_number ASC NULLS FIRST",
            countQuery = "SELECT count(*) FROM (" + UNION_SOURCE + ") r " + FILTERS)
    Page<WarrantyRecordRow> search(
            @Param("branchId") UUID branchId,
            @Param("searchPattern") String searchPattern,
            @Param("recordType") String recordType,
            @Param("warrantyStatus") String warrantyStatus,
            @Param("unitStatus") String unitStatus,
            @Param("supplier") String supplier,
            @Param("endFrom") LocalDate endFrom,
            @Param("endTo") LocalDate endTo,
            @Param("today") LocalDate today,
            @Param("soonDate") LocalDate soonDate,
            Pageable pageable);

    /** Distinct suppliers actually in use — fills the filter dropdown without a free-text guess. */
    @Query(
            nativeQuery = true,
            value =
                    "SELECT DISTINCT i.supplier FROM ces_service.inventory_items i"
                            + " WHERE i.branch_id = :branchId AND i.deleted_at IS NULL"
                            + " AND i.supplier IS NOT NULL AND i.supplier <> ''"
                            + " ORDER BY i.supplier")
    List<String> findDistinctSuppliers(@Param("branchId") UUID branchId);
}
