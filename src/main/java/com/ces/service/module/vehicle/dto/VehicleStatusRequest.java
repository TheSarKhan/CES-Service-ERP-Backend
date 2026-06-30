package com.ces.service.module.vehicle.dto;

import com.ces.service.module.vehicle.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Status-change payload (PATCH /vehicles/{id}/status). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleStatusRequest {

    @NotNull
    private VehicleStatus status;

    /** Optional note explaining the transition (e.g. IN_SERVICE override). */
    private String note;
}
