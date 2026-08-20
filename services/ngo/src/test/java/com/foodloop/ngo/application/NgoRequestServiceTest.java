package com.foodloop.ngo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.ngo.domain.NgoRequest;
import com.foodloop.ngo.domain.NgoRequestStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@SpringBootTest
@Testcontainers
class NgoRequestServiceTest {

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
    private NgoRequestService ngoRequestService;

    @Autowired
    private NgoRequirementService ngoRequirementService;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void createdRequestStartsOpen() {
        TenantContext.set(tenantId);
        NgoRequest request = ngoRequestService.create(
                tenantId, UUID.randomUUID(), "PRODUCE", new BigDecimal("30"), "KG",
                Instant.now().plus(2, ChronoUnit.DAYS), "Weekly pantry restock");

        assertThat(request.getStatus()).isEqualTo(NgoRequestStatus.OPEN);
        assertThat(request.getMatchedProposalId()).isNull();
    }

    @Test
    void markMatchedFromProposalTransitionsOpenToMatched() {
        TenantContext.set(tenantId);
        NgoRequest request = ngoRequestService.create(
                tenantId, UUID.randomUUID(), "PRODUCE", new BigDecimal("30"), "KG",
                Instant.now().plus(2, ChronoUnit.DAYS), null);
        UUID proposalId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        ngoRequestService.markMatchedFromProposal(tenantId, request.getId(), proposalId, listingId);

        TenantContext.set(tenantId);
        NgoRequest reloaded = ngoRequestService.get(request.getId());
        assertThat(reloaded.getStatus()).isEqualTo(NgoRequestStatus.MATCHED);
        assertThat(reloaded.getMatchedProposalId()).isEqualTo(proposalId);
        assertThat(reloaded.getMatchedFoodListingId()).isEqualTo(listingId);
    }

    @Test
    void markMatchedFromProposalIsIdempotentOnRedelivery() {
        TenantContext.set(tenantId);
        NgoRequest request = ngoRequestService.create(
                tenantId, UUID.randomUUID(), "PRODUCE", new BigDecimal("30"), "KG",
                Instant.now().plus(2, ChronoUnit.DAYS), null);
        UUID firstProposalId = UUID.randomUUID();
        UUID secondProposalId = UUID.randomUUID();

        ngoRequestService.markMatchedFromProposal(tenantId, request.getId(), firstProposalId, UUID.randomUUID());
        ngoRequestService.markMatchedFromProposal(tenantId, request.getId(), secondProposalId, UUID.randomUUID());

        TenantContext.set(tenantId);
        assertThat(ngoRequestService.get(request.getId()).getMatchedProposalId()).isEqualTo(firstProposalId);
    }

    @Test
    void cancelIsTerminalAndRejectsFurtherTransitions() {
        TenantContext.set(tenantId);
        NgoRequest request = ngoRequestService.create(
                tenantId, UUID.randomUUID(), "BAKERY", new BigDecimal("10"), "SERVINGS",
                Instant.now().plus(1, ChronoUnit.DAYS), null);

        NgoRequest cancelled = ngoRequestService.cancel(request.getId());
        assertThat(cancelled.getStatus()).isEqualTo(NgoRequestStatus.CANCELLED);

        assertThatThrownBy(() -> ngoRequestService.markFulfilled(request.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void requirementUpsertIsIdempotentPerOrg() {
        TenantContext.set(tenantId);
        UUID ngoOrgId = UUID.randomUUID();

        ngoRequirementService.upsert(tenantId, ngoOrgId, new String[] {"BAKERY"}, null, 100);
        var updated = ngoRequirementService.upsert(
                tenantId, ngoOrgId, new String[] {"BAKERY", "PRODUCE"}, new String[] {"HALAL"}, 150);

        var loaded = ngoRequirementService.get(ngoOrgId);
        assertThat(loaded.getId()).isEqualTo(updated.getId());
        assertThat(loaded.getPreferredCategories()).containsExactly("BAKERY", "PRODUCE");
        assertThat(loaded.getDietaryRestrictions()).containsExactly("HALAL");
        assertThat(loaded.getCapacityPerWeek()).isEqualTo(150);
    }
}
