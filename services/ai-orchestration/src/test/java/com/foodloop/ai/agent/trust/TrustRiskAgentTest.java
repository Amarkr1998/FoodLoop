package com.foodloop.ai.agent.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.ReportDto;
import com.foodloop.ai.client.RiskCaseDto;
import com.foodloop.ai.client.TrustServiceClient;
import com.foodloop.ai.client.UserBehaviorSignalDto;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.ai.provider.ChatCompletionResult;
import com.foodloop.ai.provider.ChatModelProvider;
import com.foodloop.commons.tenant.TenantContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Same approach as MatchingAgentTest: a scripted ChatModelProvider stands in for a real model; everything downstream runs for real. */
@SpringBootTest
@Testcontainers
@Import(TrustRiskAgentTest.ScriptedProviderConfig.class)
class TrustRiskAgentTest {

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
    private TrustRiskAgent trustRiskAgent;

    @Autowired
    private ScriptedChatModelProvider scriptedProvider;

    @MockBean
    private TrustServiceClient trustServiceClient;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        scriptedProvider.reset();
    }

    @Test
    void userWithNoReportsCompletesWithoutCallingTheModel() {
        UUID targetUserId = UUID.randomUUID();
        when(trustServiceClient.getSignals(eq(tenantId), eq(targetUserId)))
                .thenReturn(new UserBehaviorSignalDto(0, 0, null, Map.of()));

        TenantContext.set(tenantId);
        var result = trustRiskAgent.assess(tenantId, targetUserId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        assertThat(scriptedProvider.invocationCount()).isEqualTo(0);
    }

    @Test
    void lowRiskUserCompletesWithoutEscalating() {
        UUID targetUserId = UUID.randomUUID();
        when(trustServiceClient.getSignals(eq(tenantId), eq(targetUserId)))
                .thenReturn(new UserBehaviorSignalDto(1, 1, Instant.now(), Map.of("SPAM", 1L)));
        when(trustServiceClient.getReportHistory(eq(tenantId), eq(targetUserId)))
                .thenReturn(List.of(new ReportDto(UUID.randomUUID(), UUID.randomUUID(), targetUserId, "SPAM", "spam", Instant.now())));
        scriptedProvider.respondWith("{\"riskFactors\":\"One spam report, low severity.\"}");
        when(trustServiceClient.createRiskCase(eq(tenantId), eq(targetUserId), any()))
                .thenReturn(new RiskCaseDto(UUID.randomUUID(), targetUserId, new BigDecimal("5.00"), "One spam report.", false, "OPEN"));

        TenantContext.set(tenantId);
        var result = trustRiskAgent.assess(tenantId, targetUserId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
    }

    @Test
    void highRiskUserEscalatesForHumanReview() {
        UUID targetUserId = UUID.randomUUID();
        when(trustServiceClient.getSignals(eq(tenantId), eq(targetUserId)))
                .thenReturn(new UserBehaviorSignalDto(2, 2, Instant.now(), Map.of("SAFETY", 2L)));
        when(trustServiceClient.getReportHistory(eq(tenantId), eq(targetUserId)))
                .thenReturn(List.of(
                        new ReportDto(UUID.randomUUID(), UUID.randomUUID(), targetUserId, "SAFETY", "unsafe 1", Instant.now()),
                        new ReportDto(UUID.randomUUID(), UUID.randomUUID(), targetUserId, "SAFETY", "unsafe 2", Instant.now())));
        scriptedProvider.respondWith("{\"riskFactors\":\"Two independent safety reports.\"}");
        when(trustServiceClient.createRiskCase(eq(tenantId), eq(targetUserId), any()))
                .thenReturn(new RiskCaseDto(UUID.randomUUID(), targetUserId, new BigDecimal("55.00"), "Two safety reports.", true, "OPEN"));

        TenantContext.set(tenantId);
        var result = trustRiskAgent.assess(tenantId, targetUserId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.agentRun().isEscalated()).isTrue();
    }

    @Test
    void malformedModelOutputRetriesThenEscalatesWithoutWriting() {
        UUID targetUserId = UUID.randomUUID();
        when(trustServiceClient.getSignals(eq(tenantId), eq(targetUserId)))
                .thenReturn(new UserBehaviorSignalDto(1, 1, Instant.now(), Map.of("SPAM", 1L)));
        when(trustServiceClient.getReportHistory(eq(tenantId), eq(targetUserId)))
                .thenReturn(List.of(new ReportDto(UUID.randomUUID(), UUID.randomUUID(), targetUserId, "SPAM", "spam", Instant.now())));
        scriptedProvider.respondWith("not json");

        TenantContext.set(tenantId);
        var result = trustRiskAgent.assess(tenantId, targetUserId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(scriptedProvider.invocationCount()).isEqualTo(2);
        org.mockito.Mockito.verify(trustServiceClient, org.mockito.Mockito.never()).createRiskCase(any(), any(), any());
    }

    @TestConfiguration
    static class ScriptedProviderConfig {
        @Bean
        ScriptedChatModelProvider scriptedChatModelProvider() {
            return new ScriptedChatModelProvider();
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class ScriptedChatModelProvider implements ChatModelProvider {
        private final AtomicReference<String> response = new AtomicReference<>("");
        private final AtomicInteger invocations = new AtomicInteger();

        void respondWith(String json) {
            response.set(json);
        }

        void reset() {
            response.set("");
            invocations.set(0);
        }

        int invocationCount() {
            return invocations.get();
        }

        @Override
        public String name() {
            return "scripted-test-provider";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public ChatCompletionResult complete(String systemPrompt, String userPrompt) {
            invocations.incrementAndGet();
            return new ChatCompletionResult(name(), "scripted-model", response.get(), 0);
        }
    }
}
