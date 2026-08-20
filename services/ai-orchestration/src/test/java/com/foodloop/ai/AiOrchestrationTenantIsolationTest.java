package com.foodloop.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.ToolCallRecord;
import com.foodloop.ai.domain.ToolCallRepository;
import com.foodloop.ai.domain.ToolCallStatus;
import com.foodloop.commons.tenant.TenantContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Same proof as the other services' isolation tests, against
 * ai.agent_run / ai.tool_call: one tenant can never read another tenant's
 * agent runs or tool-call audit rows, even though nothing in the AI layer's
 * own code filters by tenant — RLS on the connection is what enforces it
 * (ADR-009).
 */
@SpringBootTest
@Testcontainers
class AiOrchestrationTenantIsolationTest {

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
    private AgentRunRepository agentRunRepository;

    @Autowired
    private ToolCallRepository toolCallRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsAgentRun() {
        AgentRun runA = createAgentRunAsTenant(tenantA);
        AgentRun runB = createAgentRunAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(agentRunRepository.findAll()).extracting(AgentRun::getId).containsExactly(runA.getId());
        assertThat(agentRunRepository.findById(runB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(agentRunRepository.findAll()).extracting(AgentRun::getId).containsExactly(runB.getId());
    }

    @Test
    void tenantCannotSeeAnotherTenantsToolCall() {
        AgentRun runA = createAgentRunAsTenant(tenantA);
        AgentRun runB = createAgentRunAsTenant(tenantB);

        TenantContext.set(tenantA);
        ToolCallRecord callA = toolCallRepository.save(new ToolCallRecord(
                tenantA, runA.getId(), "getFoodListing", "{}", "{}", "granted", ToolCallStatus.SUCCESS, 12));
        TenantContext.clear();

        TenantContext.set(tenantB);
        ToolCallRecord callB = toolCallRepository.save(new ToolCallRecord(
                tenantB, runB.getId(), "getFoodListing", "{}", "{}", "granted", ToolCallStatus.SUCCESS, 8));
        TenantContext.clear();

        TenantContext.set(tenantA);
        assertThat(toolCallRepository.findAll()).extracting(ToolCallRecord::getId).containsExactly(callA.getId());
        assertThat(toolCallRepository.findById(callB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(toolCallRepository.findAll()).extracting(ToolCallRecord::getId).containsExactly(callB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAgentRunAsTenant(tenantA);

        TenantContext.clear();
        assertThat(agentRunRepository.findAll()).isEmpty();
    }

    private AgentRun createAgentRunAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        AgentRun saved = agentRunRepository.save(new AgentRun(tenantId, "food-intelligence", UUID.randomUUID()));
        TenantContext.clear();
        return saved;
    }
}
