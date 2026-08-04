package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One transfer with its lines, names resolved. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private UUID id;
    private UUID fromNodeId;
    private String fromNodeName;
    private UUID toNodeId;
    private String toNodeName;
    private TransferStatus status;
    private String notes;
    private UUID sentBy;
    private String sentByName;
    private Instant sentAt;
    private UUID receivedBy;
    private String receivedByName;
    private Instant receivedAt;
    private Instant cancelledAt;
    private List<Line> lines;

    /** True when the viewer may receive it — false for its sender when the branch forbids that. */
    private Boolean canReceive;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {
        private UUID itemId;
        private String itemName;
        private String itemSku;
        private String unit;
        private BigDecimal quantity;
    }
}
