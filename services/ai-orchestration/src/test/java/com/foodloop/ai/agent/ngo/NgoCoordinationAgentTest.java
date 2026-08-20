package com.foodloop.ai.agent.ngo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.FoodSearchResultDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.client.NgoRequestDto;
import com.foodloop.ai.client.NgoRequirementDto;
import com.foodloop.ai.client.NgoServiceClient;
import com.foodloop.ai.client.OrganizationDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.ai.domain.PendingNgoAllocationRepository;
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
 * NgoServiceClient/TenantServiceClient/FoodServiceClient/MatchingServiceClient
 * are mocked — the boundary to other services — everything downstream (the
 * graph's routing, the escalation threshold, tool/audit persistence) runs
 * for real.
 */
@SpringBootTest
@Testcontainers
class NgoCoordinationAgentTest {

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
    private NgoCoordinationAgent ngoCoordinationAgent;

    @Autowired
    private PendingNgoAllocationRepository pendingNgoAllocationRepository;

    @MockBean
    private NgoServiceClient ngoServiceClient;

    @MockBean
    private TenantServiceClient tenantServiceClient;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private MatchingServiceClient matchingServiceClient;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void nonOpenRequestCompletesImmediatelyWithNoSearch() {
        UUID ngoRequestId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, UUID.randomUUID(), "BAKERY", new BigDecimal("10"), "SERVINGS",
                        Instant.now().plus(1, ChronoUnit.DAYS), "MATCHED"));

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(tenantServiceClient, foodServiceClient, matchingServiceClient);
    }

    @Test
    void ngoOrgWithNoLocationCompletesWithNoCandidatesFound() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, ngoOrgId, "BAKERY", new BigDecimal("10"), "SERVINGS",
                        Instant.now().plus(1, ChronoUnit.DAYS), "OPEN"));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new OrganizationDto(ngoOrgId, "Community Pantry", "NGO", null, null));

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        org.mockito.Mockito.verifyNoInteractions(foodServiceClient, matchingServiceClient);
    }

    @Test
    void belowThresholdQuantityCreatesProposalDirectly() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, ngoOrgId, "BAKERY", new BigDecimal("10"), "SERVINGS",
                        Instant.now().plus(1, ChronoUnit.DAYS), "OPEN"));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new OrganizationDto(ngoOrgId, "Community Pantry", "NGO", 12.97, 77.59));
        when(ngoServiceClient.getRequirements(eq(tenantId), eq(ngoOrgId))).thenReturn(null);
        when(foodServiceClient.searchNearby(eq(tenantId), any(Double.class), any(Double.class), any(Double.class), eq("BAKERY")))
                .thenReturn(List.of(new FoodSearchResultDto(
                        listingId, UUID.randomUUID(), "BAKERY", new BigDecimal("12"), "SERVINGS", Instant.now().plusSeconds(3600), "AVAILABLE")));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(ngoOrgId), any(), eq(ngoRequestId)))
                .thenReturn(new MatchProposalDto(UUID.randomUUID(), listingId, ngoOrgId, new BigDecimal("500"),
                        new BigDecimal("0.8"), "auto", ngoRequestId, "PROPOSED"));

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        org.mockito.Mockito.verify(matchingServiceClient).createProposal(eq(tenantId), eq(listingId), eq(ngoOrgId), any(), eq(ngoRequestId));
    }

    @Test
    void aboveThresholdQuantityEscalatesAndPersistsPendingAllocationInsteadOfProposing() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, ngoOrgId, "PRODUCE", new BigDecimal("500"), "KG",
                        Instant.now().plus(1, ChronoUnit.DAYS), "OPEN"));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new OrganizationDto(ngoOrgId, "Big Food Bank", "FOOD_BANK", 12.97, 77.59));
        when(ngoServiceClient.getRequirements(eq(tenantId), eq(ngoOrgId))).thenReturn(null);
        when(foodServiceClient.searchNearby(eq(tenantId), any(Double.class), any(Double.class), any(Double.class), eq("PRODUCE")))
                .thenReturn(List.of(new FoodSearchResultDto(
                        listingId, UUID.randomUUID(), "PRODUCE", new BigDecimal("600"), "KG", Instant.now().plusSeconds(3600), "AVAILABLE")));

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.agentRun().isEscalated()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(matchingServiceClient);

        var pending = pendingNgoAllocationRepository.findByAgentRunId(result.agentRun().getId()).orElseThrow();
        assertThat(pending.getNgoRequestId()).isEqualTo(ngoRequestId);
        assertThat(pending.getFoodListingId()).isEqualTo(listingId);
        assertThat(pending.getNgoOrgId()).isEqualTo(ngoOrgId);
    }

    @Test
    void duplicateProposalDoesNotFailTheRun() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, ngoOrgId, "BAKERY", new BigDecimal("10"), "SERVINGS",
                        Instant.now().plus(1, ChronoUnit.DAYS), "OPEN"));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new OrganizationDto(ngoOrgId, "Community Pantry", "NGO", 12.97, 77.59));
        when(ngoServiceClient.getRequirements(eq(tenantId), eq(ngoOrgId))).thenReturn(null);
        when(foodServiceClient.searchNearby(eq(tenantId), any(Double.class), any(Double.class), any(Double.class), eq("BAKERY")))
                .thenReturn(List.of(new FoodSearchResultDto(
                        listingId, UUID.randomUUID(), "BAKERY", new BigDecimal("12"), "SERVINGS", Instant.now().plusSeconds(3600), "AVAILABLE")));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(ngoOrgId), any(), eq(ngoRequestId)))
                .thenThrow(new ApiException("MATCH_ALREADY_PROPOSED", HttpStatus.CONFLICT, "already open"));

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
    }

    @Test
    void requirementsInformDietaryFilterButRequestQuantityDrivesEscalationDecision() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        when(ngoServiceClient.getRequest(eq(tenantId), eq(ngoRequestId)))
                .thenReturn(new NgoRequestDto(ngoRequestId, ngoOrgId, "PRODUCE", new BigDecimal("5"), "KG",
                        Instant.now().plus(1, ChronoUnit.DAYS), "OPEN"));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new OrganizationDto(ngoOrgId, "Community Pantry", "NGO", 12.97, 77.59));
        when(ngoServiceClient.getRequirements(eq(tenantId), eq(ngoOrgId)))
                .thenReturn(new NgoRequirementDto(ngoOrgId, new String[] {"PRODUCE"}, new String[] {"HALAL"}, 100));
        when(foodServiceClient.searchNearby(eq(tenantId), any(Double.class), any(Double.class), any(Double.class), eq("PRODUCE")))
                .thenReturn(List.of());

        TenantContext.set(tenantId);
        var result = ngoCoordinationAgent.coordinate(tenantId, ngoRequestId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        org.mockito.Mockito.verify(ngoServiceClient).getRequirements(eq(tenantId), eq(ngoOrgId));
    }
}
