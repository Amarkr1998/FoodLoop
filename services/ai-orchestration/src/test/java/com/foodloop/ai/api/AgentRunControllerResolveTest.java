package com.foodloop.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.ai.domain.PendingAllocationStatus;
import com.foodloop.ai.domain.PendingNgoAllocation;
import com.foodloop.ai.domain.PendingNgoAllocationRepository;
import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** MatchingServiceClient is mocked — everything downstream (persistence, the tool audit, the state transitions) runs for real. */
@SpringBootTest
@Testcontainers
class AgentRunControllerResolveTest {

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
    private AgentRunController agentRunController;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private PendingNgoAllocationRepository pendingNgoAllocationRepository;

    @MockBean
    private MatchingServiceClient matchingServiceClient;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void approvingCreatesTheProposalAndCompletesTheRun() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        AgentRun agentRun = createEscalatedRunWithPendingAllocation(ngoRequestId, ngoOrgId, listingId);
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(ngoOrgId), any(), eq(ngoRequestId)))
                .thenReturn(new MatchProposalDto(UUID.randomUUID(), listingId, ngoOrgId, new BigDecimal("500"),
                        new BigDecimal("0.8"), "approved", ngoRequestId, "PROPOSED"));

        TenantContext.set(tenantId);
        AgentRunResponse response = agentRunController.resolve(
                jwtWithRealmRoles(List.of("NGO_OPS")), agentRun.getId(), new ResolveEscalationRequest(true));

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED.name());
        var pending = pendingNgoAllocationRepository.findByAgentRunId(agentRun.getId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PendingAllocationStatus.APPROVED);
    }

    @Test
    void rejectingClosesTheRunWithoutCreatingAProposal() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        AgentRun agentRun = createEscalatedRunWithPendingAllocation(ngoRequestId, ngoOrgId, listingId);

        TenantContext.set(tenantId);
        AgentRunResponse response = agentRunController.resolve(
                jwtWithRealmRoles(List.of("NGO_OPS")), agentRun.getId(), new ResolveEscalationRequest(false));

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED.name());
        var pending = pendingNgoAllocationRepository.findByAgentRunId(agentRun.getId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PendingAllocationStatus.REJECTED);
        org.mockito.Mockito.verifyNoInteractions(matchingServiceClient);
    }

    @Test
    void resolvingAnAlreadyResolvedAllocationFailsFast() {
        UUID ngoRequestId = UUID.randomUUID();
        UUID ngoOrgId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        AgentRun agentRun = createEscalatedRunWithPendingAllocation(ngoRequestId, ngoOrgId, listingId);

        TenantContext.set(tenantId);
        agentRunController.resolve(jwtWithRealmRoles(List.of("NGO_OPS")), agentRun.getId(), new ResolveEscalationRequest(false));

        TenantContext.set(tenantId);
        assertThatThrownBy(() -> agentRunController.resolve(
                jwtWithRealmRoles(List.of("NGO_OPS")), agentRun.getId(), new ResolveEscalationRequest(true)))
                .isInstanceOf(ApiException.class);
    }

    private AgentRun createEscalatedRunWithPendingAllocation(UUID ngoRequestId, UUID ngoOrgId, UUID listingId) {
        TenantContext.set(tenantId);
        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, "ngo-coordination", ngoRequestId));
        agentRun.escalate("Escalated for test.");
        agentRun = agentRunRepository.save(agentRun);
        pendingNgoAllocationRepository.save(new PendingNgoAllocation(
                tenantId, agentRun.getId(), ngoRequestId, ngoOrgId, listingId, new BigDecimal("500"), "KG"));
        TenantContext.clear();
        return agentRun;
    }

    private JwtAuthenticationToken jwtWithRealmRoles(List<String> roles) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
