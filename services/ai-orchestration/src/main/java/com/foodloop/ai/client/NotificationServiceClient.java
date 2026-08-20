package com.foodloop.ai.client;

import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The Rescue Agent's path to the Notification bounded context (Phase 8) — same authenticated-boundary pattern as every other client here. */
@Component
@EnableConfigurationProperties(NotificationServiceProperties.class)
public class NotificationServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public NotificationServiceClient(NotificationServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public NotificationDto queue(
            UUID tenantId, UUID recipientOrgId, String channel, String subject, String body, UUID sourceAgentRunId) {
        return restClient.post()
                .uri("/api/v1/notifications")
                .headers(headers -> authorize(headers, tenantId))
                .body(new CreateNotificationPayload(recipientOrgId, null, channel, subject, body, sourceAgentRunId))
                .retrieve()
                .body(NotificationDto.class);
    }

    /** The Pickup Agent's sendNotification tool (spec §20) — notifying a specific volunteer, not an org. */
    public NotificationDto queueForUser(
            UUID tenantId, UUID recipientUserId, String channel, String subject, String body, UUID sourceAgentRunId) {
        return restClient.post()
                .uri("/api/v1/notifications")
                .headers(headers -> authorize(headers, tenantId))
                .body(new CreateNotificationPayload(null, recipientUserId, channel, subject, body, sourceAgentRunId))
                .retrieve()
                .body(NotificationDto.class);
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
