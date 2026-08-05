package com.ces.service.module.vehicle.repository;

import com.ces.service.module.vehicle.entity.Vehicle;
import com.ces.service.module.vehicle.enums.GarageType;
import com.ces.service.module.vehicle.enums.VehicleStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Branch-scoped repository for {@link Vehicle}. All finders exclude soft-deleted rows.
 * {@code chassis_number} uniqueness is checked globally (across all branches) per M03.3.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    Page<Vehicle> findByBranchIdAndDeletedAtIsNull(UUID branchId, Pageable pageable);

    /** chassis_number is unique across all branches (M03.3). */
    boolean existsByChassisNumberAndDeletedAtIsNull(String chassisNumber);

    boolean existsByChassisNumberAndDeletedAtIsNullAndIdNot(String chassisNumber, UUID id);

    /**
     * Filtered + searchable listing. Any of the filter params may be null (ignored when null).
     * {@code search} matches make / model / chassis_number / plate_number (case-insensitive).
     *
     * <p>The casts are load-bearing, and they have to appear at <em>every</em> use of the param,
     * not just the null check: an untyped bind gives PgJDBC nothing to infer from, so it sends
     * bytea and Postgres fails on {@code lower(bytea)}. That is why this listing errored whenever
     * no search text was typed — which was every time the page loaded.
     */
    @Query(
            """
            select v from Vehicle v
            where v.branchId = :branchId
              and v.deletedAt is null
              and (:garageType is null or v.garageType = :garageType)
              and (:status is null or v.status = :status)
              and (cast(:make as string) is null
                   or lower(v.make) like lower(concat('%', cast(:make as string), '%')))
              and (cast(:search as string) is null
                   or lower(v.make) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.model) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.chassisNumber) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.plateNumber) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Vehicle> search(
            @Param("branchId") UUID branchId,
            @Param("garageType") GarageType garageType,
            @Param("status") VehicleStatus status,
            @Param("make") String make,
            @Param("search") String search,
            Pageable pageable);
}
