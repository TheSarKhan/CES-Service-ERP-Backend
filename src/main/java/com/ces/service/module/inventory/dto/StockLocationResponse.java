package com.ces.service.module.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One folder holding a product, and how much of it sits there. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLocationResponse {

    private UUID nodeId;

    /** Folder name, resolved server-side so the UI needn't fetch each node separately. */
    private String nodeName;

    private BigDecimal quantity;
}
