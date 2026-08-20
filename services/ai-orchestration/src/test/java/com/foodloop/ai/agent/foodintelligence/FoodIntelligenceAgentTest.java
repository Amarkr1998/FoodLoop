package com.foodloop.ai.agent.foodintelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.UpdateAiMetadataPayload;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.ai.provider.ChatCompletionResult;
import com.foodloop.ai.provider.ChatModelProvider;
import com.foodloop.commons.tenant.TenantContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

/**
 * Proves the agent's real logic — not a rubber stamp: a scripted
 * {@link ChatModelProvider} test double (highest precedence, so it wins the
 * {@link com.foodloop.ai.provider.ChatModelProviderChain}) stands in for a
 * real model, exactly the seam {@link ChatModelProvider} exists for; nothing
 * about validation, retry, escalation, or the tool/audit pipeline is
 * mocked out.
 */
@SpringBootTest
@Testcontainers
@Import(FoodIntelligenceAgentTest.ScriptedProviderConfig.class)
class FoodIntelligenceAgentTest {

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
    private FoodIntelligenceAgent foodIntelligenceAgent;

    @Autowired
    private ScriptedChatModelProvider scriptedProvider;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        scriptedProvider.reset();
    }

    @Test
    void validModelOutputCompletesAndPersistsAiMetadata() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(sampleListing(listingId));
        when(foodServiceClient.updateAiMetadata(eq(tenantId), eq(listingId), any()))
                .thenReturn(sampleListing(listingId));
        scriptedProvider.respondWith("""
                {"category":"COOKED_MEAL","dietaryTypes":["VEGETARIAN"],"allergens":["dairy"],
                 "estimatedServings":4,"urgency":"MEDIUM","missingInformation":[],
                 "suggestedDescription":"Freshly cooked vegetarian meal","confidence":0.87}
                """);

        TenantContext.set(tenantId);
        FoodIntelligenceAgent.AnalysisResult result = foodIntelligenceAgent.analyze(tenantId, listingId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        assertThat(result.analysis().category()).isEqualTo("COOKED_MEAL");
        assertThat(scriptedProvider.invocationCount()).isEqualTo(1);

        verify(foodServiceClient).updateAiMetadata(eq(tenantId), eq(listingId), any(UpdateAiMetadataPayload.class));
    }

    @Test
    void unparseableModelOutputRetriesOnceThenEscalatesWithoutWriting() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(sampleListing(listingId));
        scriptedProvider.respondWith("this is not json at all");

        TenantContext.set(tenantId);
        FoodIntelligenceAgent.AnalysisResult result = foodIntelligenceAgent.analyze(tenantId, listingId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.agentRun().isEscalated()).isTrue();
        assertThat(result.analysis()).isNull();
        assertThat(scriptedProvider.invocationCount()).isEqualTo(2); // initial attempt + one bounded retry (§28)

        verify(foodServiceClient, never()).updateAiMetadata(any(), any(), any());
    }

    private FoodListingDto sampleListing(UUID id) {
        Instant now = Instant.now();
        return new FoodListingDto(
                id, "Leftover vegetable curry", "Made too much for the event",
                "COOKED_MEAL", List.of("VEGETARIAN"), List.of(),
                new BigDecimal("5"), "SERVINGS", 5,
                now.plus(4, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS),
                "DRAFT");
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
