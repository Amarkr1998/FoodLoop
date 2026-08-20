package com.foodloop.trust.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.trust.domain.ReportReason;
import com.foodloop.trust.domain.RiskCase;
import com.foodloop.trust.domain.RiskCaseStatus;
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
 * riskScore/requiresHumanReview are computed server-side from real Report
 * rows — these tests seed reports through the real ReportService and assert
 * on what RiskCaseService derives, never on a caller-supplied score.
 */
@SpringBootTest
@Testcontainers
class RiskCaseServiceTest {

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
        registry.add("foodloop.trust.human-review-threshold", () -> "50");
    }

    @Autowired
    private ReportService reportService;

    @Autowired
    private RiskCaseService riskCaseService;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void cannotOpenARiskCaseForAUserWithNoReports() {
        TenantContext.set(tenantId);
        assertThatThrownBy(() -> riskCaseService.create(tenantId, UUID.randomUUID(), "no signal"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void lowSeverityReportStaysBelowHumanReviewThreshold() {
        UUID targetUserId = UUID.randomUUID();
        TenantContext.set(tenantId);
        reportService.create(tenantId, UUID.randomUUID(), targetUserId, ReportReason.SPAM, "spam");

        RiskCase riskCase = riskCaseService.create(tenantId, targetUserId, "One low-severity spam report.");

        assertThat(riskCase.getRiskScore()).isEqualByComparingTo("5.00");
        assertThat(riskCase.isRequiresHumanReview()).isFalse();
        assertThat(riskCase.getStatus()).isEqualTo(RiskCaseStatus.OPEN);
    }

    @Test
    void multipleHighSeverityReportsCrossHumanReviewThreshold() {
        UUID targetUserId = UUID.randomUUID();
        TenantContext.set(tenantId);
        reportService.create(tenantId, UUID.randomUUID(), targetUserId, ReportReason.SAFETY, "unsafe 1");
        reportService.create(tenantId, UUID.randomUUID(), targetUserId, ReportReason.SAFETY, "unsafe 2");

        RiskCase riskCase = riskCaseService.create(tenantId, targetUserId, "Two independent safety reports.");

        assertThat(riskCase.isRequiresHumanReview()).isTrue();
    }

    @Test
    void resolvingClosesTheCaseAndRejectsASecondResolution() {
        UUID targetUserId = UUID.randomUUID();
        TenantContext.set(tenantId);
        reportService.create(tenantId, UUID.randomUUID(), targetUserId, ReportReason.SAFETY, "unsafe");
        RiskCase riskCase = riskCaseService.create(tenantId, targetUserId, "Safety concern.");

        UUID reviewerId = UUID.randomUUID();
        RiskCase resolved = riskCaseService.resolve(riskCase.getId(), "Warned user; no further action.", reviewerId);

        assertThat(resolved.getStatus()).isEqualTo(RiskCaseStatus.RESOLVED);
        assertThat(resolved.getResolvedByUserId()).isEqualTo(reviewerId);
        assertThatThrownBy(() -> riskCaseService.resolve(riskCase.getId(), "again", reviewerId))
                .isInstanceOf(ApiException.class);
    }
}
