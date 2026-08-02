package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /inventory/items/{id}/move payload. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveItemRequest {

    @NotNull
    private UUID nodeId;
}
