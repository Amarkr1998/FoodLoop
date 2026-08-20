package com.foodloop.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.matching.application.MatchingService;
import com.foodloop.matching.client.FoodListingDto;
import com.foodloop.matching.client.FoodServiceClient;
import com.foodloop.matching.client.OrganizationDto;
import com.foodloop.matching.client.TenantServiceClient;
import com.foodloop.matching.domain.MatchProposal;
import com.foodloop.matching.domain.MatchProposalRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
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

/** Same proof as every other service's isolation test, against matching.match_proposal. */
@SpringBootTest
@Testcontainers
class MatchProposalTenantIsolationTest {

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

    @Autowired
    private MatchProposalRepository matchProposalRepository;

    @MockBean
    private FoodServiceClient foodServiceClient;

    @MockBean
    private TenantServiceClient tenantServiceClient;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsMatchProposal() {
        MatchProposal proposalA = createAsTenant(tenantA);
        MatchProposal proposalB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(matchProposalRepository.findAll()).extracting(MatchProposal::getId).containsExactly(proposalA.getId());
        assertThat(matchProposalRepository.findById(proposalB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(matchProposalRepository.findAll()).extracting(MatchProposal::getId).containsExactly(proposalB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(matchProposalRepository.findAll()).isEmpty();
    }

    private MatchProposal createAsTenant(UUID tenantId) {
        UUID listingId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, "AVAILABLE", Instant.now().plusSeconds(3600), 12.97, 77.59));
        when(tenantServiceClient.getOrganization(eq(tenantId), eq(orgId)))
                .thenReturn(new OrganizationDto(orgId, "NGO", "NGO", 12.9705, 77.5905));

        TenantContext.set(tenantId);
        MatchProposal saved = matchingService.createProposal(tenantId, listingId, orgId, null);
        TenantContext.clear();
        return saved;
    }
}
