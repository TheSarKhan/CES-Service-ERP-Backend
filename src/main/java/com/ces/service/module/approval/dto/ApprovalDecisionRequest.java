package com.ces.service.module.approval.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Optional note attached to an approve / reject decision. */
@Getter
@Setter
public class ApprovalDecisionRequest {

    @Size(max = 2000)
    private String note;
}
