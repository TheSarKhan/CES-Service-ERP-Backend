package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "Zəmanəti uzat" payload. Either add {@code months} to the current end date or name an absolute
 * {@code newEndDate}; the service rejects a request that gives neither.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyExtendRequest {

    private Integer months;

    private LocalDate newEndDate;

    @Size(max = 2000)
    private String reason;
}
