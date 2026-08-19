package com.foodloop.pickup;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.pickup.domain.GeoUtils;
import com.foodloop.pickup.domain.PickupTask;
import com.foodloop.pickup.domain.PickupTaskRepository;
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
 * Same proof as the other services' isolation tests, against
 * pickup.pickup_task: one tenant can never read another tenant's tasks.
 */
@SpringBootTest
@Testcontainers
class PickupTaskTenantIsolationTest {

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
    private PickupTaskRepository pickupTaskRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsPickupTask() {
        PickupTask taskA = createAsTenant(tenantA);
        PickupTask taskB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(pickupTaskRepository.findAll()).extracting(PickupTask::getId).containsExactly(taskA.getId());
        assertThat(pickupTaskRepository.findById(taskB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(pickupTaskRepository.findAll()).extracting(PickupTask::getId).containsExactly(taskB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(pickupTaskRepository.findAll()).isEmpty();
    }

    private PickupTask createAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        Instant now = Instant.now();
        PickupTask saved = pickupTaskRepository.save(new PickupTask(
                tenantId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), GeoUtils.point(12.9716, 77.5946)));
        TenantContext.clear();
        return saved;
    }
}
