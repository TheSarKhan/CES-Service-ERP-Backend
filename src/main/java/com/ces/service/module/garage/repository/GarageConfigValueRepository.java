package com.ces.service.module.garage.repository;

import com.ces.service.module.garage.entity.GarageConfigValue;
import com.ces.service.module.garage.enums.GarageConfigListType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarageConfigValueRepository extends JpaRepository<GarageConfigValue, UUID> {

    Optional<GarageConfigValue> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    List<GarageConfigValue> findByBranchIdAndListTypeAndDeletedAtIsNullOrderBySortOrderAsc(
            UUID branchId, GarageConfigListType listType);

    List<GarageConfigValue> findByBranchIdAndDeletedAtIsNullOrderByListTypeAscSortOrderAsc(UUID branchId);

    Optional<GarageConfigValue> findByBranchIdAndListTypeAndValueAndDeletedAtIsNull(
            UUID branchId, GarageConfigListType listType, String value);

    boolean existsByBranchIdAndListTypeAndValueAndDeletedAtIsNull(
            UUID branchId, GarageConfigListType listType, String value);

    boolean existsByBranchIdAndListTypeAndValueAndDeletedAtIsNullAndIdNot(
            UUID branchId, GarageConfigListType listType, String value, UUID excludeId);
}
