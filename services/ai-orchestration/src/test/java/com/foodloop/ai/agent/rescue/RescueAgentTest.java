package com.foodloop.ai.agent.rescue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.client.NotificationDto;
import com.foodloop.ai.client.NotificationServiceClient;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * FoodServiceClient/MatchingServiceClient/NotificationServiceClient are
 * mocked — the boundary to other services — everything downstream (the
 * graph's routing, the T-1h-always-escalates policy, tool/audit
 * persistence) runs for real.
 */
@SpringBootTest
@Testcontainers
class RescueAgentTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

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
    private RescueAgent rescueAgent;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private MatchingServiceClient matchingServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void listingNoLongerAvailableCompletesWithoutSearchingOrNotifying() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(claimedListing(listingId));

        TenantContext.set(tenantId);
        RescueAgent.RescueResult result = rescueAgent.check(tenantId, listingId, RescueThreshold.T_MINUS_4H);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        verify(matchingServiceClient, never()).getCandidates(any(), any(), anyDouble());
        verify(notificationServiceClient, never()).queue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void t4hWithCandidatesNotifiesAndProposesButDoesNotEscalate() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(availableListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), eq(10.0)))
                .thenReturn(List.of(new MatchCandidateDto(orgId, "Nearby NGO", 500, new BigDecimal("0.8"))));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(orgId), any()))
                .thenReturn(new MatchProposalDto(UUID.randomUUID(), listingId, orgId,
                        new BigDecimal("500"), new BigDecimal("0.8"), "auto", "PROPOSED"));
        when(notificationServiceClient.queue(eq(tenantId), eq(orgId), any(), any(), any(), any()))
                .thenReturn(new NotificationDto(UUID.randomUUID(), orgId, "IN_APP", "subj", "QUEUED"));

        TenantContext.set(tenantId);
        RescueAgent.RescueResult result = rescueAgent.check(tenantId, listingId, RescueThreshold.T_MINUS_4H);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        verify(notificationServiceClient, times(1)).queue(eq(tenantId), eq(orgId), any(), any(), any(), any());
        verify(matchingServiceClient, times(1)).createProposal(eq(tenantId), eq(listingId), eq(orgId), any());
    }

    @Test
    void t1hAlwaysEscalatesEvenWhenNotificationAndProposalSucceed() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(availableListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), eq(25.0)))
                .thenReturn(List.of(new MatchCandidateDto(orgId, "Wider NGO", 20000, new BigDecimal("0.4"))));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(orgId), any()))
                .thenReturn(new MatchProposalDto(UUID.randomUUID(), listingId, orgId,
                        new BigDecimal("20000"), new BigDecimal("0.4"), "auto", "PROPOSED"));
        when(notificationServiceClient.queue(eq(tenantId), eq(orgId), any(), any(), any(), any()))
                .thenReturn(new NotificationDto(UUID.randomUUID(), orgId, "IN_APP", "subj", "QUEUED"));

        TenantContext.set(tenantId);
        RescueAgent.RescueResult result = rescueAgent.check(tenantId, listingId, RescueThreshold.T_MINUS_1H);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.agentRun().isEscalated()).isTrue();
    }

    @Test
    void noCandidatesAtT4hCompletesButAtT1hEscalates() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId4h = UUID.randomUUID();
        UUID listingId1h = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId4h))).thenReturn(availableListing(listingId4h));
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId1h))).thenReturn(availableListing(listingId1h));
        when(matchingServiceClient.getCandidates(eq(tenantId), any(), anyDouble())).thenReturn(List.of());

        TenantContext.set(tenantId);
        RescueAgent.RescueResult resultAt4h = rescueAgent.check(tenantId, listingId4h, RescueThreshold.T_MINUS_4H);
        RescueAgent.RescueResult resultAt1h = rescueAgent.check(tenantId, listingId1h, RescueThreshold.T_MINUS_1H);

        assertThat(resultAt4h.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(resultAt1h.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
    }

    @Test
    void duplicateMatchProposalDoesNotFailTheRun() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(availableListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), eq(10.0)))
                .thenReturn(List.of(new MatchCandidateDto(orgId, "Nearby NGO", 500, new BigDecimal("0.8"))));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(orgId), any()))
                .thenThrow(new ApiException("MATCH_ALREADY_PROPOSED", HttpStatus.CONFLICT, "already open"));
        when(notificationServiceClient.queue(eq(tenantId), eq(orgId), any(), any(), any(), any()))
                .thenReturn(new NotificationDto(UUID.randomUUID(), orgId, "IN_APP", "subj", "QUEUED"));

        TenantContext.set(tenantId);
        RescueAgent.RescueResult result = rescueAgent.check(tenantId, listingId, RescueThreshold.T_MINUS_4H);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        verify(notificationServiceClient, times(1)).queue(eq(tenantId), eq(orgId), any(), any(), any(), any());
    }

    private FoodListingDto availableListing(UUID id) {
        Instant now = Instant.now();
        return new FoodListingDto(
                id, "Leftover vegetable curry", "Made too much for the event",
                "COOKED_MEAL", List.of("VEGETARIAN"), List.of(),
                new BigDecimal("5"), "SERVINGS", 5,
                now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS),
                "AVAILABLE");
    }

    private FoodListingDto claimedListing(UUID id) {
        Instant now = Instant.now();
        return new FoodListingDto(
                id, "Leftover vegetable curry", "Made too much for the event",
                "COOKED_MEAL", List.of("VEGETARIAN"), List.of(),
                new BigDecimal("5"), "SERVINGS", 5,
                now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS),
                "CLAIMED");
    }
}
