package com.foodloop.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.identity.domain.AppUser;
import com.foodloop.identity.domain.AppUserRepository;
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
 * Proves the exact requirement from docs/architecture/06-security-threat-model.md
 * (T1) and the spec's §31 mandate for a dedicated cross-tenant leakage test:
 * against the real {@code identity.app_user} table and its RLS policy
 * (V1__create_app_user.sql), one tenant can never read or overwrite another
 * tenant's row, and a request with no tenant established sees nothing.
 */
@SpringBootTest
@Testcontainers
class AppUserTenantIsolationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @BeforeAll
    static void createUnprivilegedAppRole() throws Exception {
        // Deliberately NOT the container's bootstrap user (a Postgres
        // superuser, which unconditionally bypasses row-level security) —
        // see infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
        // and ADR-009. Testing against the superuser would pass even if the
        // RLS policy did nothing at all.
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
        // Deliberately NOT the container's bootstrap user (a Postgres
        // superuser, which unconditionally bypasses row-level security) —
        // see create-app-role.sql and ADR-009. Testing against the
        // superuser would pass even if the RLS policy did nothing at all.
        registry.add("spring.datasource.username", () -> "app_test");
        registry.add("spring.datasource.password", () -> "app_test_only");
        // Avoids OIDC-discovery-on-startup against issuer-uri, which would
        // otherwise try to reach a real Keycloak this test doesn't start —
        // nothing here exercises JWT validation, only the RLS mechanism.
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "https://example.invalid/jwks");
    }

    @Autowired
    private AppUserRepository appUserRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsUser() {
        AppUser userA = saveAsTenant(tenantA, "a@example.com");
        AppUser userB = saveAsTenant(tenantB, "b@example.com");

        TenantContext.set(tenantA);
        assertThat(appUserRepository.findAll()).extracting(AppUser::getId).containsExactly(userA.getId());
        assertThat(appUserRepository.findById(userB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(appUserRepository.findAll()).extracting(AppUser::getId).containsExactly(userB.getId());
        assertThat(appUserRepository.findById(userA.getId())).isEmpty();
    }

    @Test
    void noTenantSetSeesNoRows() {
        saveAsTenant(tenantA, "c@example.com");

        TenantContext.clear();
        assertThat(appUserRepository.findAll()).isEmpty();
    }

    private AppUser saveAsTenant(UUID tenantId, String email) {
        TenantContext.set(tenantId);
        AppUser saved = appUserRepository.save(new AppUser(tenantId, UUID.randomUUID(), email, "Test User", "en"));
        TenantContext.clear();
        return saved;
    }
}
