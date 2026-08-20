package com.foodloop.ai.agent.pickup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.NotificationDto;
import com.foodloop.ai.client.NotificationServiceClient;
import com.foodloop.ai.client.PickupServiceClient;
import com.foodloop.ai.client.PickupTaskDto;
import com.foodloop.ai.client.VolunteerProfileDto;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.commons.tenant.TenantContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** PickupServiceClient/NotificationServiceClient are mocked — the boundary to other services — everything downstream runs for real. */
@SpringBootTest
@Testcontainers
class PickupAgentTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @BeforeAll
    static void createUnprivilegedAppRole() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE app_test WITH LOGIN PASSWORD 'app_test_only' NOSUPERUSER NOBYPASSRLS");
            statement.execute("GRANT CREATE ON DATABASE " + POSTGRES.getDatabaseName() + " TO app_test");
        }
    }

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "app_test");
        registry.add("spring.datasource.password", () -> "app_test_only");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "https://example.invalid/jwks");
    }

    @Autowired
    private PickupAgent pickupAgent;

    @MockBean
    private PickupServiceClient pickupServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void taskAlreadyArrivedCompletesWithoutSearchingOrNotifying() {
        UUID taskId = UUID.randomUUID();
        UUID volunteerUserId = UUID.randomUUID();
        when(pickupServiceClient.getTask(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, volunteerUserId, "ARRIVED", Instant.now(), Instant.now(), 12.97, 77.59));

        TenantContext.set(tenantId);
        var result = pickupAgent.checkDelay(tenantId, taskId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        verify(pickupServiceClient, never()).findNearbyVolunteers(any(), any(), any(Double.class));
    }

    @Test
    void delayedTaskWithNoAssignedVolunteerCompletesWithoutSearching() {
        UUID taskId = UUID.randomUUID();
        when(pickupServiceClient.getTask(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, null, "UNASSIGNED", Instant.now(), Instant.now(), 12.97, 77.59));

        TenantContext.set(tenantId);
        var result = pickupAgent.checkDelay(tenantId, taskId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        verify(pickupServiceClient, never()).findNearbyVolunteers(any(), any(), any(Double.class));
    }

    @Test
    void delayedTaskWithAvailableReplacementsNotifiesAndReassigns() {
        UUID taskId = UUID.randomUUID();
        UUID assignedVolunteerId = UUID.randomUUID();
        UUID replacementUserId = UUID.randomUUID();
        when(pickupServiceClient.getTask(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, assignedVolunteerId, "EN_ROUTE", Instant.now(), Instant.now(), 12.97, 77.59));
        when(pickupServiceClient.findNearbyVolunteers(eq(tenantId), eq(taskId), any(Double.class)))
                .thenReturn(List.of(new VolunteerProfileDto(UUID.randomUUID(), replacementUserId, "BICYCLE", 10, true, 12.98, 77.60)));
        when(notificationServiceClient.queueForUser(eq(tenantId), eq(replacementUserId), any(), any(), any(), any()))
                .thenReturn(new NotificationDto(UUID.randomUUID(), null, replacementUserId, "PUSH", "subj", "QUEUED"));
        when(pickupServiceClient.systemUnassign(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, null, "UNASSIGNED", Instant.now(), Instant.now(), 12.97, 77.59));

        TenantContext.set(tenantId);
        var result = pickupAgent.checkDelay(tenantId, taskId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        verify(notificationServiceClient, times(1)).queueForUser(eq(tenantId), eq(replacementUserId), any(), any(), any(), any());
        verify(pickupServiceClient, times(1)).systemUnassign(eq(tenantId), eq(taskId));
    }

    @Test
    void delayedTaskExcludesTheCurrentlyAssignedVolunteerFromCandidates() {
        UUID taskId = UUID.randomUUID();
        UUID assignedVolunteerId = UUID.randomUUID();
        when(pickupServiceClient.getTask(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, assignedVolunteerId, "ASSIGNED", Instant.now(), Instant.now(), 12.97, 77.59));
        // Only the assigned volunteer themself shows up as "available" (e.g. still marked available, just unresponsive).
        when(pickupServiceClient.findNearbyVolunteers(eq(tenantId), eq(taskId), any(Double.class)))
                .thenReturn(List.of(new VolunteerProfileDto(UUID.randomUUID(), assignedVolunteerId, "CAR", 20, true, 12.97, 77.59)));

        TenantContext.set(tenantId);
        var result = pickupAgent.checkDelay(tenantId, taskId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        verify(notificationServiceClient, never()).queueForUser(any(), any(), any(), any(), any(), any());
        verify(pickupServiceClient, never()).systemUnassign(any(), any());
    }

    @Test
    void delayedTaskWithNoReplacementsEscalates() {
        UUID taskId = UUID.randomUUID();
        UUID assignedVolunteerId = UUID.randomUUID();
        when(pickupServiceClient.getTask(eq(tenantId), eq(taskId)))
                .thenReturn(new PickupTaskDto(taskId, assignedVolunteerId, "ASSIGNED", Instant.now(), Instant.now(), 12.97, 77.59));
        when(pickupServiceClient.findNearbyVolunteers(eq(tenantId), eq(taskId), any(Double.class)))
                .thenReturn(List.of());

        TenantContext.set(tenantId);
        var result = pickupAgent.checkDelay(tenantId, taskId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.agentRun().isEscalated()).isTrue();
    }
}
