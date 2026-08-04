package com.ces.service.module.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Transfer göndər" — one or more products leaving one folder for another. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotNull
    private UUID fromNodeId;

    @NotNull
    private UUID toNodeId;

    @NotEmpty
    @Valid
    private List<Line> lines;

    @Size(max = 2000)
    private String notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {

        @NotNull
        private UUID itemId;

        @NotNull
        @DecimalMin(value = "0.001")
        private BigDecimal quantity;
    }
}
