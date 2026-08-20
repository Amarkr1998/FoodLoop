package com.foodloop.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.food.application.FoodListingService;
import com.foodloop.food.domain.FoodAiMetadata;
import com.foodloop.food.domain.FoodListing;
import com.foodloop.food.domain.FoodListingRepository;
import com.foodloop.food.domain.FoodVerificationStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
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
 * Proves the Food Intelligence Agent's write path end-to-end at the
 * persistence layer: {@code ai_metadata} round-trips through the JSONB
 * column for real, {@code verification_status} advances to
 * {@code AI_REVIEWED}, the donor's own fields are untouched, and the
 * DRAFT-only guard (FoodListing.recordAiMetadata) actually rejects a
 * published listing rather than silently accepting the write.
 */
@SpringBootTest
@Testcontainers
class FoodAiMetadataTest {

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
    void recordsAiMetadataOnDraftListingAndAdvancesVerificationStatus() {
        TenantContext.set(tenantId);
        FoodListing listing = foodListingService.createDraft(
                tenantId, UUID.randomUUID(), FoodTestSupport.sampleRequest(UUID.randomUUID()));

        FoodAiMetadata metadata = new FoodAiMetadata(
                "COOKED_MEAL", List.of("VEGETARIAN"), List.of("dairy"), 6, "MEDIUM",
                List.of("pickup window seems short"), "Freshly cooked vegetarian meal", 0.9, Instant.now());
        foodListingService.applyAiMetadata(listing.getId(), metadata);

        FoodListing reloaded = foodListingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getVerificationStatus()).isEqualTo(FoodVerificationStatus.AI_REVIEWED);
        assertThat(reloaded.getAiMetadata()).isNotNull();
        assertThat(reloaded.getAiMetadata().category()).isEqualTo("COOKED_MEAL");
        assertThat(reloaded.getAiMetadata().confidence()).isEqualTo(0.9);
        // The donor's own fields are never overwritten by the agent's suggestions.
        assertThat(reloaded.getEstimatedServings()).isEqualTo(20);
    }

    @Test
    void rejectsAiMetadataOnceListingIsPublished() {
        TenantContext.set(tenantId);
        FoodListing listing = foodListingService.createDraft(
                tenantId, UUID.randomUUID(), FoodTestSupport.sampleRequest(UUID.randomUUID()));
        UUID donorUserId = listing.getDonorUserId();
        foodListingService.publish(listing.getId(), donorUserId);

        FoodAiMetadata metadata = new FoodAiMetadata(
                "COOKED_MEAL", List.of(), List.of(), null, "LOW", List.of(), null, 0.5, Instant.now());

        assertThatThrownBy(() -> foodListingService.applyAiMetadata(listing.getId(), metadata))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DRAFT");
    }
}
