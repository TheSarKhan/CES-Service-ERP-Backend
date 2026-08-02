package com.ces.service.module.inventory.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Result of resolving a scanned QR/barcode value to whatever it identifies. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLookupResponse {

    /** NODE | ITEM | ITEM_UNIT */
    private String type;
    private UUID id;
}
