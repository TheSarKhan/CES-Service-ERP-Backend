package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.StockMovement;
import com.ces.service.module.inventory.enums.StockMovementType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
            String referenceType, UUID referenceId);

    /**
     * Ledger listing. Split by which filters are present rather than one query with
     * {@code (:x is null or ...)} clauses: the enum comparison hits the same PgJDBC type-inference
     * limitation documented on {@link InventoryItemUnitRepository}.
     */
    @Query("select m from StockMovement m where m.branchId = :branchId "
            + "and (:itemId is null or m.itemId = :itemId) "
            + "and (:nodeId is null or m.nodeId = :nodeId)")
    Page<StockMovement> search(
            @Param("branchId") UUID branchId,
            @Param("itemId") UUID itemId,
            @Param("nodeId") UUID nodeId,
            Pageable pageable);

    @Query("select m from StockMovement m where m.branchId = :branchId "
            + "and m.movementType = :movementType "
            + "and (:itemId is null or m.itemId = :itemId) "
            + "and (:nodeId is null or m.nodeId = :nodeId)")
    Page<StockMovement> searchByType(
            @Param("branchId") UUID branchId,
            @Param("itemId") UUID itemId,
            @Param("nodeId") UUID nodeId,
            @Param("movementType") StockMovementType movementType,
            Pageable pageable);
}
