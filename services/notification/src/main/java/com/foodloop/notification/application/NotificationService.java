package com.foodloop.notification.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.notification.domain.Notification;
import com.foodloop.notification.domain.NotificationChannel;
import com.foodloop.notification.domain.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification queue(
            UUID tenantId, UUID recipientOrgId, NotificationChannel channel, String subject, String body,
            UUID sourceAgentRunId) {
        return notificationRepository.save(new Notification(tenantId, recipientOrgId, channel, subject, body, sourceAgentRunId));
    }

    @Transactional(readOnly = true)
    public Notification get(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No notification found with id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<Notification> listForRecipient(UUID recipientOrgId) {
        return notificationRepository.findByRecipientOrgId(recipientOrgId);
    }
}
