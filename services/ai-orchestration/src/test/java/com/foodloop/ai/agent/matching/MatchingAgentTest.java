package com.foodloop.ai.agent.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.MatchingServiceClient;
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
 * Same approach as FoodIntelligenceAgentTest: a scripted {@link ChatModelProvider}
 * stands in for a real model, everything downstream (validation, the
 * "chosen candidate must be one Matching itself returned" check, retry,
 * escalation, tool/audit pipeline) runs for real.
 */
@SpringBootTest
@Testcontainers
@Import(MatchingAgentTest.ScriptedProviderConfig.class)
class MatchingAgentTest {

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
    private MatchingAgent matchingAgent;

    @Autowired
    private ScriptedChatModelProvider scriptedProvider;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private MatchingServiceClient matchingServiceClient;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        scriptedProvider.reset();
    }

    @Test
    void validChoiceAmongGivenCandidatesCompletesAndPersistsProposal() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID chosenOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(sampleListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(List.of(
                        new MatchCandidateDto(chosenOrgId, "Close NGO", 400, new BigDecimal("0.9")),
                        new MatchCandidateDto(otherOrgId, "Far NGO", 8000, new BigDecimal("0.3"))));
        when(matchingServiceClient.createProposal(eq(tenantId), eq(listingId), eq(chosenOrgId), any()))
                .thenReturn(new MatchProposalDto(UUID.randomUUID(), listingId, chosenOrgId,
                        new BigDecimal("400"), new BigDecimal("0.9"), "Closest and highest scored.", "PROPOSED"));
        scriptedProvider.respondWith("{\"receiverOrgId\":\"" + chosenOrgId + "\",\"rationale\":\"Closest and highest scored.\"}");

        TenantContext.set(tenantId);
        MatchingAgent.SuggestionResult result = matchingAgent.suggest(tenantId, listingId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.proposal().receiverOrgId()).isEqualTo(chosenOrgId);
        assertThat(scriptedProvider.invocationCount()).isEqualTo(1);
    }

    @Test
    void choosingACandidateNotInTheGivenSetIsRejectedAndEscalatesWithoutWriting() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID realCandidateId = UUID.randomUUID();
        UUID hallucinatedId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(sampleListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(List.of(new MatchCandidateDto(realCandidateId, "Only NGO", 400, new BigDecimal("0.9"))));
        // The model "hallucinates" an org id that was never offered as a candidate.
        scriptedProvider.respondWith("{\"receiverOrgId\":\"" + hallucinatedId + "\",\"rationale\":\"trust me\"}");

        TenantContext.set(tenantId);
        MatchingAgent.SuggestionResult result = matchingAgent.suggest(tenantId, listingId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(result.proposal()).isNull();
        assertThat(scriptedProvider.invocationCount()).isEqualTo(2); // initial attempt + one bounded retry
        org.mockito.Mockito.verify(matchingServiceClient, never()).createProposal(any(), any(), any(), any());
    }

    @Test
    void noCandidatesCompletesWithoutEscalatingOrCallingTheModel() {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId))).thenReturn(sampleListing(listingId));
        when(matchingServiceClient.getCandidates(eq(tenantId), eq(listingId), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(List.of());

        TenantContext.set(tenantId);
        MatchingAgent.SuggestionResult result = matchingAgent.suggest(tenantId, listingId);

        assertThat(result.agentRun().getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(result.agentRun().isEscalated()).isFalse();
        assertThat(result.proposal()).isNull();
        assertThat(scriptedProvider.invocationCount()).isEqualTo(0);
    }

    private FoodListingDto sampleListing(UUID id) {
        Instant now = Instant.now();
        return new FoodListingDto(
                id, "Leftover vegetable curry", "Made too much for the event",
                "COOKED_MEAL", List.of("VEGETARIAN"), List.of(),
                new BigDecimal("5"), "SERVINGS", 5,
                now.plus(4, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS),
                "AVAILABLE");
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
