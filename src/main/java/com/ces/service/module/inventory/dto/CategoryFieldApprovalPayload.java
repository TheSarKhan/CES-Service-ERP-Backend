package com.ces.service.module.inventory.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parked payload for a category-field change. Field operations address two ids (the category and
 * the field), so the field id travels alongside the body rather than in the request path.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryFieldApprovalPayload {

    /** Null for FIELD_ADD — the field doesn't exist yet. */
    private UUID fieldId;

    /** Null for FIELD_DELETE — nothing is being written. */
    private InventoryCategoryFieldRequest field;
}
