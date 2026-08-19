package com.foodloop.pickup;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.pickup.application.PickupService;
import com.foodloop.pickup.domain.PickupTaskRepository;
import com.foodloop.pickup.infrastructure.events.FoodClaimedEvent;
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

/**
 * Proves the actual guarantee that matters for the Kafka consumer (§7:
 * "idempotent consumers"): a redelivered food.claimed.v1 event — Kafka's
 * at-least-once delivery makes this a routine occurrence, not an edge case
 * — must not create a second pickup task for the same claim.
 */
@SpringBootTest
@Testcontainers
class CreateFromClaimIdempotencyTest {

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
    private PickupService pickupService;

    @Autowired
    private PickupTaskRepository pickupTaskRepository;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void redeliveredEventDoesNotCreateASecondTask() {
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();
        FoodClaimedEvent event = new FoodClaimedEvent(
                UUID.randomUUID(), "FOOD_CLAIMED", 1, tenantId, now, "food-service",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), 12.9716, 77.5946);

        pickupService.createFromClaim(event);
        pickupService.createFromClaim(event);

        TenantContext.set(tenantId);
        try {
            assertThat(pickupTaskRepository.findAll()).hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }
}
