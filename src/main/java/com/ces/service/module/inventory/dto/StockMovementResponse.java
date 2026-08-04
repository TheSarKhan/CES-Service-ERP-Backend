package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.StockMovement;
import com.ces.service.module.inventory.enums.StockMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One line of stock history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponse {

    private UUID id;
    private UUID itemId;
    private String itemName;
    private UUID nodeId;
    /** Resolved server-side; a history that only shows folder ids is not history. */
    private String nodeName;
    private UUID unitId;
    private StockMovementType movementType;
    /** Signed: positive brought stock in, negative took it out. */
    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private String referenceType;
    private UUID referenceId;
    private String reason;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;

    public static StockMovementResponse from(
            StockMovement movement, String itemName, String nodeName, String createdByName) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .itemId(movement.getItemId())
                .itemName(itemName)
                .nodeId(movement.getNodeId())
                .nodeName(nodeName)
                .unitId(movement.getUnitId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .balanceAfter(movement.getBalanceAfter())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .reason(movement.getReason())
                .createdBy(movement.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
