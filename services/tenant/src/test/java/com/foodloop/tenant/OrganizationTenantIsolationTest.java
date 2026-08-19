package com.foodloop.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.tenant.domain.Organization;
import com.foodloop.tenant.domain.OrganizationRepository;
import com.foodloop.tenant.domain.OrganizationType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * Same proof as identity's AppUserTenantIsolationTest, against the
 * tenant.organization table and its RLS policy: one tenant can never read
 * or overwrite another tenant's organizations, and no tenant set means no
 * rows (fail closed).
 */
@SpringBootTest
@Testcontainers
class OrganizationTenantIsolationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @BeforeAll
    static void createUnprivilegedAppRole() throws Exception {
        // See identity's AppUserTenantIsolationTest for why this must not
        // be the Testcontainers bootstrap superuser (ADR-009).
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
    private OrganizationRepository organizationRepository;

    @Autowired
    private javax.sql.DataSource dataSource;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void seedTenants() throws Exception {
        // organization.tenant_id has a real FK to tenant.tenant(id) — the
        // two tables live in the same schema/service, unlike the
        // cross-context references elsewhere in this platform, so a DB-
        // enforced FK here is appropriate rather than a boundary violation.
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO tenant.tenant (id, name, region_id, country_code) VALUES "
                    + "('" + tenantA + "', 'Tenant A', 'TEST', 'IN'), "
                    + "('" + tenantB + "', 'Tenant B', 'TEST', 'IN')");
        }
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsOrganization() {
        Organization orgA = saveAsTenant(tenantA, "Tenant A Restaurant");
        Organization orgB = saveAsTenant(tenantB, "Tenant B Restaurant");

        TenantContext.set(tenantA);
        assertThat(organizationRepository.findAll()).extracting(Organization::getId).containsExactly(orgA.getId());
        assertThat(organizationRepository.findById(orgB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(organizationRepository.findAll()).extracting(Organization::getId).containsExactly(orgB.getId());
        assertThat(organizationRepository.findById(orgA.getId())).isEmpty();
    }

    @Test
    void noTenantSetSeesNoRows() {
        saveAsTenant(tenantA, "Tenant A NGO");

        TenantContext.clear();
        assertThat(organizationRepository.findAll()).isEmpty();
    }

    private Organization saveAsTenant(UUID tenantId, String name) {
        TenantContext.set(tenantId);
        Organization saved = organizationRepository.save(new Organization(tenantId, name, OrganizationType.DONOR_RESTAURANT));
        TenantContext.clear();
        return saved;
    }
}
