package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    /**
     * Filtered + searchable listing. Every text filter param must be wrapped in
     * {@code CAST(:x AS string)} at EVERY use, not only the null check — a bare {@code :x IS NULL}
     * gives PgJDBC nothing to infer the parameter's type from and it sends the wire type as bytea,
     * which {@code lower(bytea)} then rejects. This bit the old vehicles listing before it was
     * rebuilt; the cast is what actually fixed it, not just adding the null guard.
     *
     * <p>{@code garageType} is the one exception: it's bound as the actual {@link GarageType} enum
     * (not a String), so Hibernate resolves its JDBC type from the entity attribute mapping instead
     * of needing the string-cast trick — casting a bind parameter to an arbitrary Java enum class
     * name in JPQL isn't valid and made Hibernate emit a bogus {@code cast(? as smallint)}.
     */
    @Query("""
            select v from Vehicle v
            where v.branchId = :branchId
              and v.deletedAt is null
              and (:garageType is null or v.garageType = :garageType)
              and (cast(:status as string) is null or v.status = cast(:status as string))
              and (cast(:vehicleType as string) is null or v.vehicleType = cast(:vehicleType as string))
              and (cast(:make as string) is null or lower(v.make) like lower(concat('%', cast(:make as string), '%')))
              and (cast(:model as string) is null or lower(v.model) like lower(concat('%', cast(:model as string), '%')))
              and (cast(:location as string) is null or v.currentLocation = cast(:location as string))
              and (:ownerId is null or v.ownerId = :ownerId)
              and (:usesEngineHours is null or v.usesEngineHours = :usesEngineHours)
              and (:usesKm is null or v.usesKm = :usesKm)
              and (cast(:search as string) is null
                   or lower(v.code) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.make) like lower(concat('%', cast(:search as string), '%'))
                   or lower(v.model) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(v.chassisNumber, '')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(v.serialNumber, '')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(v.plateNumber, '')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(v.notes, '')) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Vehicle> search(
            @Param("branchId") UUID branchId,
            @Param("garageType") GarageType garageType,
            @Param("status") String status,
            @Param("vehicleType") String vehicleType,
            @Param("make") String make,
            @Param("model") String model,
            @Param("location") String location,
            @Param("ownerId") UUID ownerId,
            @Param("usesEngineHours") Boolean usesEngineHours,
            @Param("usesKm") Boolean usesKm,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByChassisNumberAndDeletedAtIsNull(String chassisNumber);

    boolean existsByChassisNumberAndDeletedAtIsNullAndIdNot(String chassisNumber, UUID excludeId);

    boolean existsBySerialNumberAndDeletedAtIsNull(String serialNumber);

    boolean existsBySerialNumberAndDeletedAtIsNullAndIdNot(String serialNumber, UUID excludeId);

    boolean existsByPlateNumberAndDeletedAtIsNull(String plateNumber);

    boolean existsByPlateNumberAndDeletedAtIsNullAndIdNot(String plateNumber, UUID excludeId);

    List<Vehicle> findByBranchIdAndDeletedAtIsNull(UUID branchId);
}
