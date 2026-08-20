package com.foodloop.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
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
 * Proves the Safety Agent's hold is a real, enforced gate (spec §22): a
 * flagged listing's publish attempt fails even for its own donor, and only
 * clearing the hold (never publishing around it) lets publish succeed again.
 */
@SpringBootTest
@Testcontainers
class FoodSafetyReviewTest {

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

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void flaggedListingCannotBePublishedUntilTheHoldIsCleared() {
        TenantContext.set(tenantId);
        FoodListing listing = foodListingService.createDraft(
                tenantId, UUID.randomUUID(), FoodTestSupport.sampleRequest(UUID.randomUUID()));
        UUID donorUserId = listing.getDonorUserId();

        foodListingService.flagForSafetyReview(listing.getId(), "Description mentions a medical claim.");

        assertThatThrownBy(() -> foodListingService.publish(listing.getId(), donorUserId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("safety review");

        FoodListing flagged = foodListingRepository.findById(listing.getId()).orElseThrow();
        assertThat(flagged.isRequiresSafetyReview()).isTrue();
        assertThat(flagged.getSafetyReviewReason()).contains("medical claim");
        assertThat(flagged.getStatus().name()).isEqualTo("DRAFT");

        foodListingService.clearSafetyReview(listing.getId());
        FoodListing published = foodListingService.publish(listing.getId(), donorUserId);

        assertThat(published.getStatus().name()).isEqualTo("AVAILABLE");
        assertThat(published.isRequiresSafetyReview()).isFalse();
    }
}
