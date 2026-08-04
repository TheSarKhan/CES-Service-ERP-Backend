package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.StocktakeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A counting sheet. While it is OPEN the lines carry no system figures at all. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StocktakeResponse {

    private UUID id;
    private UUID nodeId;
    private String nodeName;
    private StocktakeStatus status;
    private String notes;
    private UUID approvalRequestId;
    private UUID openedBy;
    private String openedByName;
    private Instant openedAt;
    private Instant closedAt;
    private Instant appliedAt;

    private int lineCount;
    private int countedCount;
    /** Only meaningful once closed; zero while counting, because variance is still hidden. */
    private int varianceCount;

    private List<Line> lines;

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
        /**
         * Null while the sheet is open — this is what makes the count blind. It appears only once
         * counting is finished and the variance report is the point of the screen.
         */
        private BigDecimal systemQuantity;
        private BigDecimal countedQuantity;
        /** counted − system; null until both are known. */
        private BigDecimal variance;
        private String notes;
    }
}
