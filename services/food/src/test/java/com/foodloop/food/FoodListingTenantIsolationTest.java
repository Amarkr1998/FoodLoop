package com.foodloop.food;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.food.application.FoodListingService;
import com.foodloop.food.domain.FoodListing;
import com.foodloop.food.domain.FoodListingRepository;
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
 * Same proof as identity's/tenant's isolation tests, against
 * food.food_listing: one tenant can never read another tenant's listings.
 */
@SpringBootTest
@Testcontainers
class FoodListingTenantIsolationTest {

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
    private FoodListingService foodListingService;

    @Autowired
    private FoodListingRepository foodListingRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsListing() {
        FoodListing listingA = createAsTenant(tenantA);
        FoodListing listingB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(foodListingRepository.findAll()).extracting(FoodListing::getId).containsExactly(listingA.getId());
        assertThat(foodListingRepository.findById(listingB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(foodListingRepository.findAll()).extracting(FoodListing::getId).containsExactly(listingB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(foodListingRepository.findAll()).isEmpty();
    }

    private FoodListing createAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        FoodListing saved = foodListingService.createDraft(tenantId, UUID.randomUUID(), FoodTestSupport.sampleRequest(UUID.randomUUID()));
        TenantContext.clear();
        return saved;
    }
}
