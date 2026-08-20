package com.foodloop.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.matching.application.MatchCandidate;
import com.foodloop.matching.application.MatchingService;
import com.foodloop.matching.client.FoodListingDto;
import com.foodloop.matching.client.FoodServiceClient;
import com.foodloop.matching.client.OrganizationDto;
import com.foodloop.matching.client.TenantServiceClient;
import com.foodloop.matching.domain.MatchProposal;
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
 * FoodServiceClient/TenantServiceClient are mocked — the boundary to other
 * services — but everything downstream of them (MatchingEngine scoring,
 * the eligibility re-validation, RLS-backed persistence) runs for real, so
 * these tests prove createProposal genuinely can't be talked into an
 * ineligible match no matter what an agent's input claims.
 */
@SpringBootTest
@Testcontainers
class MatchingServiceTest {

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
    private MatchingService matchingService;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private TenantServiceClient tenantServiceClient;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void createsProposalWithServerComputedScoreAndDistance() {
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant expiry = Instant.now().plus(2, ChronoUnit.HOURS);
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "AVAILABLE", expiry, 12.9716, 77.5946));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(orgId)))
                .thenReturn(new OrganizationDto(orgId, "Nearby NGO", "NGO", 12.9720, 77.5950));

        TenantContext.set(tenantId);
        MatchProposal proposal = matchingService.createProposal(tenantId, listingId, orgId, "Close and fresh — good fit.");

        assertThat(proposal.getFoodListingId()).isEqualTo(listingId);
        assertThat(proposal.getReceiverOrgId()).isEqualTo(orgId);
        assertThat(proposal.getDistanceMeters().doubleValue()).isGreaterThan(0).isLessThan(1000);
        assertThat(proposal.getScore().doubleValue()).isBetween(0.0, 1.0);
        assertThat(proposal.getAiRationale()).isEqualTo("Close and fresh — good fit.");
    }

    @Test
    void rejectsProposalWhenListingIsNotAvailable() {
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "CLAIMED", Instant.now().plusSeconds(3600), 0, 0));

        TenantContext.set(tenantId);
        assertThatThrownBy(() -> matchingService.createProposal(tenantId, listingId, orgId, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AVAILABLE");
    }

    @Test
    void rejectsProposalWhenOrgIsNotReceiverCapable() {
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "AVAILABLE", Instant.now().plusSeconds(3600), 12.97, 77.59));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(orgId)))
                .thenReturn(new OrganizationDto(orgId, "A Donor Restaurant", "DONOR_RESTAURANT", 12.97, 77.59));

        TenantContext.set(tenantId);
        assertThatThrownBy(() -> matchingService.createProposal(tenantId, listingId, orgId, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("receiver-capable");
    }

    @Test
    void rejectsDuplicateActiveProposalForTheSamePair() {
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "AVAILABLE", Instant.now().plusSeconds(3600), 12.97, 77.59));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(orgId)))
                .thenReturn(new OrganizationDto(orgId, "Nearby NGO", "NGO", 12.9705, 77.5905));

        TenantContext.set(tenantId);
        matchingService.createProposal(tenantId, listingId, orgId, "first");

        assertThatThrownBy(() -> matchingService.createProposal(tenantId, listingId, orgId, "second"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already open");
    }

    @Test
    void findCandidatesRanksByDeterministicScoreDescending() {
        UUID listingId = UUID.randomUUID();
        UUID closeOrgId = UUID.randomUUID();
        UUID farOrgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "AVAILABLE", Instant.now().plusSeconds(3600), 12.9716, 77.5946));
        when(tenantServiceClient.searchNearbyReceivers(eq(tenantId), any(Double.class), any(Double.class), any(Double.class), eq(null)))
                .thenReturn(List.of(
                        new OrganizationDto(farOrgId, "Far NGO", "NGO", 13.05, 77.65),
                        new OrganizationDto(closeOrgId, "Close NGO", "NGO", 12.9720, 77.5950)));

        TenantContext.set(tenantId);
        List<MatchCandidate> candidates = matchingService.findCandidates(tenantId, listingId, 10.0);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).receiverOrgId()).isEqualTo(closeOrgId);
        assertThat(candidates.get(0).score()).isGreaterThan(candidates.get(1).score());
    }
}
