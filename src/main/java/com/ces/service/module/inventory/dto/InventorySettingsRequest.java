package com.ces.service.module.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Update payload for the warehouse settings. Null fields are left as they are. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySettingsRequest {

    @Size(max = 50)
    private List<@Email @Size(max = 255) String> notificationEmails;

    private Boolean dailyDigestEnabled;
}
