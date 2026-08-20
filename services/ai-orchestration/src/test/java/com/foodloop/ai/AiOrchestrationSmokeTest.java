package com.foodloop.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.ToolCallRepository;
import com.foodloop.ai.domain.ToolCallStatus;
import com.foodloop.ai.graph.AgentGraph;
import com.foodloop.ai.graph.GraphNode;
import com.foodloop.ai.provider.ChatModelProviderChain;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.commons.tenant.TenantContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
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
 * Proves the Phase 5 foundation actually works end-to-end wired together as
 * Spring beans — provider fallback, the permission-checked tool lifecycle,
 * the graph engine's routing, and audit persistence — rather than each
 * piece only being exercised in isolation. This is deliberately internal
 * only (no REST endpoint): there is no real business agent yet, so exposing
 * this as an API would be exactly the "fake feature ahead of need" the spec
 * forbids (§63). It exists solely to prove the scaffolding a real agent
 * will be built on is genuine, not a stub.
 */
@SpringBootTest
@Testcontainers
class AiOrchestrationSmokeTest {

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

    @Autowired
    private ToolExecutor toolExecutor;

    @Autowired
    private ChatModelProviderChain chatModelProviderChain;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    /** Minimal state carried through the graph: what the tool returned, and the model's rationale. */
    private record SmokeState(UUID listingId, Map<String, Object> toolResult, String modelRationale) {
    }

    @Test
    void graphToolProviderAndAuditPipelineWorksEndToEnd() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, "food-intelligence", UUID.randomUUID()));
        AgentCallerContext caller = new AgentCallerContext("food-intelligence", tenantId, agentRun.getId());
        UUID listingId = UUID.randomUUID();

        AgentGraph<SmokeState> graph = AgentGraph.<SmokeState>builder("retrieve")
                .node(retrieveNode(caller))
                .node(reasonNode())
                .edge("retrieve", state -> "reason")
                .edge("reason", state -> AgentGraph.END)
                .build();

        SmokeState result = graph.run(new SmokeState(listingId, null, null));

        assertThat(result.toolResult()).containsEntry("status", "PUBLISHED");
        assertThat(result.modelRationale()).isNotBlank();

        agentRun.recordModel("mock", "mock-model");
        agentRun.complete("smoke test: retrieved listing, generated rationale");
        agentRunRepository.save(agentRun);

        assertThat(toolCallRepository.findByAgentRunId(agentRun.getId()))
                .hasSize(1)
                .first()
                .satisfies(call -> {
                    assertThat(call.getToolName()).isEqualTo("getFoodListing");
                    assertThat(call.getStatus()).isEqualTo(ToolCallStatus.SUCCESS);
                });
    }

    @Test
    void toolCallOutsideAgentsPermissionMatrixIsDeniedAndAudited() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, "pickup", UUID.randomUUID()));
        AgentCallerContext caller = new AgentCallerContext("pickup", tenantId, agentRun.getId());

        // Pickup's permission-matrix row (AgentPermissionRegistry) does not include getFoodListing.
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                        com.foodloop.commons.web.ApiException.class,
                        () -> toolExecutor.run(new FakeGetFoodListingTool(), caller, UUID.randomUUID()))
                .getCode())
                .isEqualTo("TOOL_NOT_PERMITTED");

        assertThat(toolCallRepository.findByAgentRunId(agentRun.getId()))
                .hasSize(1)
                .first()
                .satisfies(call -> assertThat(call.getStatus()).isEqualTo(ToolCallStatus.DENIED));
    }

    private GraphNode<SmokeState> retrieveNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "retrieve";
            }

            @Override
            public SmokeState execute(SmokeState state) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) (Map<String, ?>)
                        toolExecutor.run(new FakeGetFoodListingTool(), caller, state.listingId());
                return new SmokeState(state.listingId(), output, state.modelRationale());
            }
        };
    }

    private GraphNode<SmokeState> reasonNode() {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "reason";
            }

            @Override
            public SmokeState execute(SmokeState state) {
                var completion = chatModelProviderChain.complete(
                        "You summarize food listing status for a donor.",
                        "Listing status: " + state.toolResult().get("status"));
                return new SmokeState(state.listingId(), state.toolResult(), completion.content());
            }
        };
    }

    /** A stand-in for the real Food Intelligence Agent's getFoodListing tool, built only to prove the pipeline. */
    private static final class FakeGetFoodListingTool implements AgentTool<UUID, Map<String, Object>> {

        @Override
        public String name() {
            return "getFoodListing";
        }

        @Override
        public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
            return AuthorizationResult.allow();
        }

        @Override
        public void validateInput(UUID input) {
            if (input == null) {
                throw new IllegalArgumentException("listingId must not be null");
            }
        }

        @Override
        public Map<String, Object> execute(UUID input) {
            return Map.of("id", input.toString(), "status", "PUBLISHED");
        }

        @Override
        public void validateOutput(Map<String, Object> output) {
            if (!output.containsKey("status")) {
                throw new IllegalStateException("tool output missing required 'status' field");
            }
        }
    }
}
