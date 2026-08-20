package com.foodloop.trust;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.trust.application.ReportService;
import com.foodloop.trust.domain.Report;
import com.foodloop.trust.domain.ReportReason;
import com.foodloop.trust.domain.ReportRepository;
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

/** Same proof as every other service's isolation test, against trust.report. */
@SpringBootTest
@Testcontainers
class ReportTenantIsolationTest {

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
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsReport() {
        Report reportA = createAsTenant(tenantA);
        Report reportB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(reportRepository.findAll()).extracting(Report::getId).containsExactly(reportA.getId());
        assertThat(reportRepository.findById(reportB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(reportRepository.findAll()).extracting(Report::getId).containsExactly(reportB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(reportRepository.findAll()).isEmpty();
    }

    private Report createAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        Report saved = reportService.create(tenantId, UUID.randomUUID(), UUID.randomUUID(), ReportReason.SPAM, "test");
        TenantContext.clear();
        return saved;
    }
}
