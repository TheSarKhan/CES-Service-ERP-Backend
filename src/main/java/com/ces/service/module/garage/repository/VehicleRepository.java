package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.Vehicle;
import com.ces.service.module.garage.enums.GarageType;
import java.math.BigDecimal;
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
     * Filtered + searchable listing. {@code garageType}/{@code status}/{@code vehicleType}/
     * {@code make}/{@code model} are multi-select — the frontend's Filtrlər dialog lets a user
     * pick several of each, so these match via SQL {@code IN} against a list rather than equality
     * against one value. The service passes {@code null} (never an empty list) when nothing is
     * selected — Hibernate does support {@code x in ()}, but not every JPA provider does, so this
     * codebase doesn't rely on it.
     *
     * <p>The remaining text filter ({@code location}) and the free-text {@code search} still need
     * {@code CAST(:x AS string)} at every use, not only the null check — a bare {@code :x IS NULL}
     * gives PgJDBC nothing to infer the parameter's type from and it sends the wire type as bytea,
     * which {@code lower(bytea)} then rejects. This bit the old vehicles listing before it was
     * rebuilt; the cast is what actually fixed it, not just adding the null guard.
     */
    @Query("""
            select v from Vehicle v
            where v.branchId = :branchId
              and v.deletedAt is null
              and (:garageTypes is null or v.garageType in :garageTypes)
              and (:statuses is null or v.status in :statuses)
              and (:vehicleTypes is null or v.vehicleType in :vehicleTypes)
              and (:makes is null or v.make in :makes)
              and (:models is null or v.model in :models)
              and (cast(:location as string) is null or v.currentLocation = cast(:location as string))
              and (:ownerId is null or v.ownerId = :ownerId)
              and (:usesEngineHours is null or v.usesEngineHours = :usesEngineHours)
              and (:usesKm is null or v.usesKm = :usesKm)
              and (:purchasePriceMin is null or v.purchasePrice >= :purchasePriceMin)
              and (:purchasePriceMax is null or v.purchasePrice <= :purchasePriceMax)
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
            @Param("garageTypes") List<GarageType> garageTypes,
            @Param("statuses") List<String> statuses,
            @Param("vehicleTypes") List<String> vehicleTypes,
            @Param("makes") List<String> makes,
            @Param("models") List<String> models,
            @Param("location") String location,
            @Param("ownerId") UUID ownerId,
            @Param("usesEngineHours") Boolean usesEngineHours,
            @Param("usesKm") Boolean usesKm,
            @Param("purchasePriceMin") BigDecimal purchasePriceMin,
            @Param("purchasePriceMax") BigDecimal purchasePriceMax,
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
