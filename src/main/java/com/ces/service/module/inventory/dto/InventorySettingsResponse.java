package com.ces.service.module.inventory.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Per-branch warehouse settings, as the configuration screen sees them. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySettingsResponse {

    /** Who the daily low-stock digest reaches. Empty means nothing is sent. */
    private List<String> notificationEmails;

    private Boolean dailyDigestEnabled;

    /** When true, a transfer cannot be received by the person who sent it. */
    private Boolean transferRequiresDifferentReceiver;

    /**
     * False when no SMTP host is configured on the server: the screen has to say so, otherwise
     * somebody types addresses in and waits for mail that was never going to be sent.
     */
    private Boolean mailConfigured;
}
