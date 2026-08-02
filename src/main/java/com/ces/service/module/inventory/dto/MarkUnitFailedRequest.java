package com.ces.service.module.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /inventory/item-units/{id}/fail payload — records a within/out-of-warranty failure. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkUnitFailedRequest {

    private String failureNotes;
}
