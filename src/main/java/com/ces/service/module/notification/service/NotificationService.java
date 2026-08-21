package com.ces.service.module.notification.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.notification.dto.NotificationResponse;
import com.ces.service.module.notification.entity.Notification;
import com.ces.service.module.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Boolean isRead, Pageable pageable) {
        UUID userId = currentUserId();
        Page<Notification> page = isRead != null
                ? notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUserId());
    }

    public NotificationResponse markAsRead(UUID id) {
        UUID userId = currentUserId();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        if (!userId.equals(notification.getRecipientId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        if (Boolean.FALSE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(currentUserId());
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
    }
}
