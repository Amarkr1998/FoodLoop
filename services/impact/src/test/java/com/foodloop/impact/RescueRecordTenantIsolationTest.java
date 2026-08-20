package com.foodloop.impact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.impact.application.ImpactService;
import com.foodloop.impact.client.FoodListingDto;
import com.foodloop.impact.client.FoodServiceClient;
import com.foodloop.impact.domain.RescueRecord;
import com.foodloop.impact.domain.RescueRecordRepository;
import com.foodloop.impact.infrastructure.events.PickupCompletedEvent;
import java.math.BigDecimal;
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

/** Same proof as every other service's isolation test, against impact.rescue_record. */
@SpringBootTest
@Testcontainers
class RescueRecordTenantIsolationTest {

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
    private ImpactService impactService;

    @Autowired
    private RescueRecordRepository rescueRecordRepository;

    @MockBean
    private FoodServiceClient foodServiceClient;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsRescueRecord() {
        RescueRecord recordA = createAsTenant(tenantA);
        RescueRecord recordB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(rescueRecordRepository.findAll()).extracting(RescueRecord::getId).containsExactly(recordA.getId());
        assertThat(rescueRecordRepository.findById(recordB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(rescueRecordRepository.findAll()).extracting(RescueRecord::getId).containsExactly(recordB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(rescueRecordRepository.findAll()).isEmpty();
    }

    private RescueRecord createAsTenant(UUID tenantId) {
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(any(), any()))
                .thenReturn(new FoodListingDto(listingId, UUID.randomUUID(), "PRODUCE", new BigDecimal("5"), "KG"));

        impactService.recordFromPickupCompleted(new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, Instant.now(), "pickup-service",
                UUID.randomUUID(), listingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        TenantContext.set(tenantId);
        RescueRecord saved = rescueRecordRepository.findAll().get(0);
        TenantContext.clear();
        return saved;
    }
}
