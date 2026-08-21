package com.ces.service.module.notification.dto;

import com.ces.service.module.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID branchId;
    private UUID recipientId;
    private String eventType;
    private String title;
    private String body;
    private String refType;
    private UUID refId;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .branchId(n.getBranchId())
                .recipientId(n.getRecipientId())
                .eventType(n.getEventType())
                .title(n.getTitle())
                .body(n.getBody())
                .refType(n.getRefType())
                .refId(n.getRefId())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
