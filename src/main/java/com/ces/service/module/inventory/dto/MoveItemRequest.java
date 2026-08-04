package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /inventory/items/{id}/move payload — relocates everything held at one folder to another.
 *
 * <p>The source is explicit: a product may sit in several folders, so "move it" has to say which
 * stock is being moved.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveItemRequest {

    @NotNull
    private UUID fromNodeId;

    @NotNull
    private UUID toNodeId;
}
