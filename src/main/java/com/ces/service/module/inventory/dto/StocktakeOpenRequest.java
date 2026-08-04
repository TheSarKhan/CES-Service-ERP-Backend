package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Sayıma başla" — one folder at a time, so a sheet matches what one person can walk. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StocktakeOpenRequest {

    @NotNull
    private UUID nodeId;

    @Size(max = 2000)
    private String notes;
}
