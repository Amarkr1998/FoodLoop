package com.foodloop.ngo;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.ngo.application.NgoRequestService;
import com.foodloop.ngo.domain.NgoRequest;
import com.foodloop.ngo.domain.NgoRequestRepository;
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

/** Same proof as every other service's isolation test, against ngo.ngo_request. */
@SpringBootTest
@Testcontainers
class NgoRequestTenantIsolationTest {

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
    private NgoRequestRepository ngoRequestRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsNgoRequest() {
        NgoRequest requestA = createAsTenant(tenantA);
        NgoRequest requestB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(ngoRequestRepository.findAll()).extracting(NgoRequest::getId).containsExactly(requestA.getId());
        assertThat(ngoRequestRepository.findById(requestB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(ngoRequestRepository.findAll()).extracting(NgoRequest::getId).containsExactly(requestB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(ngoRequestRepository.findAll()).isEmpty();
    }

    private NgoRequest createAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        NgoRequest saved = ngoRequestService.create(
                tenantId, UUID.randomUUID(), "BAKERY", new BigDecimal("50"), "SERVINGS",
                Instant.now().plus(3, ChronoUnit.DAYS), null);
        TenantContext.clear();
        return saved;
    }
}
