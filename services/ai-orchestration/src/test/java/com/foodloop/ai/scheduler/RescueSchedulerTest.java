package com.foodloop.ai.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodloop.ai.agent.rescue.RescueAgent;
import com.foodloop.ai.agent.rescue.RescueThreshold;
import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.TenantDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.commons.tenant.TenantContext;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * RescueAgent itself is mocked here — its behavior is RescueAgentTest's job
 * — so this only proves the scheduler's own responsibility: bucketing by
 * actual remaining time, and skipping a listing/threshold pair the audit
 * trail already shows was handled.
 */
@SpringBootTest
@Testcontainers
class RescueSchedulerTest {

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
    private RescueScheduler rescueScheduler;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @MockBean
    private TenantServiceClient tenantServiceClient;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private RescueAgent rescueAgent;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void bucketsByRemainingTimeAndInvokesAgentForEachUnhandledListing() {
        UUID urgentListingId = UUID.randomUUID();
        UUID relaxedListingId = UUID.randomUUID();
        Instant now = Instant.now();
        when(tenantServiceClient.listActiveTenants()).thenReturn(List.of(new TenantDto(tenantId, "Tenant A", "ACTIVE")));
        when(foodServiceClient.getExpiringListings(eq(tenantId), eq(240))).thenReturn(List.of(
                listingExpiringIn(urgentListingId, now, 30),
                listingExpiringIn(relaxedListingId, now, 200)));
        when(rescueAgent.check(any(), any(), any())).thenReturn(
                new RescueAgent.RescueResult(new AgentRun(tenantId, "rescue", urgentListingId)));

        rescueScheduler.sweep();

        verify(rescueAgent).check(tenantId, urgentListingId, RescueThreshold.T_MINUS_1H);
        verify(rescueAgent).check(tenantId, relaxedListingId, RescueThreshold.T_MINUS_4H);
    }

    @Test
    void skipsAListingAlreadyHandledAtThatThreshold() {
        UUID listingId = UUID.randomUUID();
        Instant now = Instant.now();
        when(tenantServiceClient.listActiveTenants()).thenReturn(List.of(new TenantDto(tenantId, "Tenant A", "ACTIVE")));
        when(foodServiceClient.getExpiringListings(eq(tenantId), eq(240)))
                .thenReturn(List.of(listingExpiringIn(listingId, now, 200)));

        TenantContext.set(tenantId);
        AgentRun priorRun = new AgentRun(tenantId, "rescue", listingId);
        priorRun.complete("[T_MINUS_4H] Notified 2 nearby receiver(s).");
        agentRunRepository.save(priorRun);
        TenantContext.clear();

        rescueScheduler.sweep();

        verify(rescueAgent, never()).check(any(), any(), any());
    }

    private FoodListingDto listingExpiringIn(UUID id, Instant now, int minutes) {
        return new FoodListingDto(
                id, "Leftover curry", "desc", "COOKED_MEAL", List.of(), List.of(),
                new BigDecimal("5"), "SERVINGS", 5,
                now.plus(minutes, ChronoUnit.MINUTES), now, now.plus(6, ChronoUnit.HOURS), "AVAILABLE");
    }
}
