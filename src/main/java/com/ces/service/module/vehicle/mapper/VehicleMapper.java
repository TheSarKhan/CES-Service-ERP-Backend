package com.ces.service.module.vehicle.mapper;

import com.ces.service.module.vehicle.dto.VehicleDocumentRequest;
import com.ces.service.module.vehicle.dto.VehicleDocumentResponse;
import com.ces.service.module.vehicle.dto.VehicleRequest;
import com.ces.service.module.vehicle.dto.VehicleResponse;
import com.ces.service.module.vehicle.entity.Vehicle;
import com.ces.service.module.vehicle.entity.VehicleDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for the Vehicle module (M03). Demonstrates the standard entity &harr; DTO pattern.
 * Audit / identity fields (id, branchId, status defaults, engine hours, timestamps) are managed by
 * the service layer / persistence and are deliberately ignored on inbound mappings.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VehicleMapper {

    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentEngineHours", ignore = true)
    @Mapping(target = "lastEngineHoursAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentEngineHours", ignore = true)
    @Mapping(target = "lastEngineHoursAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(VehicleRequest request, @MappingTarget Vehicle vehicle);

    VehicleDocumentResponse toDocumentResponse(VehicleDocument document);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    VehicleDocument toDocumentEntity(VehicleDocumentRequest request);
}
