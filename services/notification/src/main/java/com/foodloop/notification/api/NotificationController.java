package com.foodloop.notification.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.notification.application.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/api/v1/notifications")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        boolean hasOrg = request.recipientOrgId() != null;
        boolean hasUser = request.recipientUserId() != null;
        if (hasOrg == hasUser) {
            throw new ApiException("INVALID_RECIPIENT", HttpStatus.BAD_REQUEST,
                    "Exactly one of recipientOrgId or recipientUserId must be set.");
        }
        var notification = hasUser
                ? notificationService.queueForUser(
                        currentTenantId(), request.recipientUserId(), request.channel(), request.subject(), request.body(),
                        request.sourceAgentRunId())
                : notificationService.queueForOrg(
                        currentTenantId(), request.recipientOrgId(), request.channel(), request.subject(), request.body(),
                        request.sourceAgentRunId());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(notification));
    }

    @GetMapping("/api/v1/notifications/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return NotificationResponse.from(notificationService.get(id));
    }

    @GetMapping("/api/v1/notifications")
    public List<NotificationResponse> listForRecipient(@RequestParam UUID recipientOrgId) {
        return notificationService.listForRecipient(recipientOrgId).stream().map(NotificationResponse::from).toList();
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return tenantId;
    }
}
