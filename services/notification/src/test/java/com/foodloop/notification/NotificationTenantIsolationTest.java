package com.foodloop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.notification.application.NotificationService;
import com.foodloop.notification.domain.Notification;
import com.foodloop.notification.domain.NotificationChannel;
import com.foodloop.notification.domain.NotificationRepository;
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

/** Same proof as every other service's isolation test, against notification.notification. */
@SpringBootTest
@Testcontainers
class NotificationTenantIsolationTest {

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
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantCannotSeeAnotherTenantsNotification() {
        Notification notificationA = createAsTenant(tenantA);
        Notification notificationB = createAsTenant(tenantB);

        TenantContext.set(tenantA);
        assertThat(notificationRepository.findAll()).extracting(Notification::getId).containsExactly(notificationA.getId());
        assertThat(notificationRepository.findById(notificationB.getId())).isEmpty();

        TenantContext.set(tenantB);
        assertThat(notificationRepository.findAll()).extracting(Notification::getId).containsExactly(notificationB.getId());
    }

    @Test
    void noTenantSetSeesNoRows() {
        createAsTenant(tenantA);

        TenantContext.clear();
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    private Notification createAsTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        Notification saved = notificationService.queue(
                tenantId, UUID.randomUUID(), NotificationChannel.IN_APP, "Food expiring soon",
                "A nearby listing is expiring soon.", null);
        TenantContext.clear();
        return saved;
    }
}
