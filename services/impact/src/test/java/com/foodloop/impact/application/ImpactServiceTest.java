package com.foodloop.impact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.impact.client.FoodListingDto;
import com.foodloop.impact.client.FoodServiceClient;
import com.foodloop.impact.domain.CategoryImpactSummary;
import com.foodloop.impact.domain.ImpactSummary;
import com.foodloop.impact.domain.MonthlyImpactSummary;
import com.foodloop.impact.domain.RescueRecordRepository;
import com.foodloop.impact.infrastructure.events.PickupCompletedEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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

/**
 * FoodServiceClient is mocked — the boundary to another service — but
 * everything downstream (kg/CO2e computation, idempotent persistence,
 * RLS-backed aggregate reads) runs for real against Postgres.
 */
@SpringBootTest
@Testcontainers
class ImpactServiceTest {

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

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void recordsRescueWithComputedKgAndCo2FromTheListingsQuantity() {
        UUID pickupTaskId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID donorOrgId = UUID.randomUUID();
        UUID donorUserId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, donorOrgId, "BAKERY", new BigDecimal("10"), "SERVINGS"));

        impactService.recordFromPickupCompleted(new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, Instant.now(), "pickup-service",
                pickupTaskId, listingId, UUID.randomUUID(), donorUserId, receiverUserId));

        TenantContext.set(tenantId);
        ImpactSummary donorSummary = impactService.getDonorImpact(donorUserId);
        assertThat(donorSummary.rescueCount()).isEqualTo(1);
        assertThat(donorSummary.totalKgSaved()).isEqualByComparingTo("4.000");
        assertThat(donorSummary.totalCo2SavedKg()).isEqualByComparingTo("10.000");

        ImpactSummary receiverSummary = impactService.getReceiverImpact(receiverUserId);
        assertThat(receiverSummary.rescueCount()).isEqualTo(1);

        ImpactSummary orgSummary = impactService.getOrgImpact(donorOrgId);
        assertThat(orgSummary.rescueCount()).isEqualTo(1);

        ImpactSummary communitySummary = impactService.getCommunityImpact();
        assertThat(communitySummary.rescueCount()).isEqualTo(1);
    }

    @Test
    void redeliveredEventDoesNotDoubleCountImpact() {
        UUID pickupTaskId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID donorUserId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, UUID.randomUUID(), "PRODUCE", new BigDecimal("2"), "KG"));

        PickupCompletedEvent event = new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, Instant.now(), "pickup-service",
                pickupTaskId, listingId, UUID.randomUUID(), donorUserId, UUID.randomUUID());

        impactService.recordFromPickupCompleted(event);
        impactService.recordFromPickupCompleted(event);

        TenantContext.set(tenantId);
        assertThat(impactService.getDonorImpact(donorUserId).rescueCount()).isEqualTo(1);
        verify(foodServiceClient, times(1)).getFoodListing(eq(tenantId), eq(listingId));
    }

    @Test
    void userWithNoRescuesGetsZeroedSummaryNotAnError() {
        TenantContext.set(tenantId);
        ImpactSummary summary = impactService.getDonorImpact(UUID.randomUUID());

        assertThat(summary.rescueCount()).isEqualTo(0);
        assertThat(summary.totalKgSaved()).isEqualByComparingTo("0");
        assertThat(summary.totalCo2SavedKg()).isEqualByComparingTo("0");
    }

    @Test
    void categoryBreakdownGroupsByFoodCategoryOrderedByKgSavedDescending() {
        UUID donorOrgId = UUID.randomUUID();
        recordRescueAs(donorOrgId, "BAKERY", new BigDecimal("1"), "KG", Instant.now());
        recordRescueAs(donorOrgId, "PRODUCE", new BigDecimal("5"), "KG", Instant.now());
        recordRescueAs(donorOrgId, "PRODUCE", new BigDecimal("5"), "KG", Instant.now());

        TenantContext.set(tenantId);
        List<CategoryImpactSummary> orgBreakdown = impactService.getOrgCategoryBreakdown(donorOrgId);
        assertThat(orgBreakdown).hasSize(2);
        assertThat(orgBreakdown.get(0).foodCategory()).isEqualTo("PRODUCE");
        assertThat(orgBreakdown.get(0).rescueCount()).isEqualTo(2);
        assertThat(orgBreakdown.get(0).totalKgSaved()).isEqualByComparingTo("10.000");
        assertThat(orgBreakdown.get(1).foodCategory()).isEqualTo("BAKERY");

        List<CategoryImpactSummary> communityBreakdown = impactService.getCommunityCategoryBreakdown();
        assertThat(communityBreakdown).hasSize(2);
    }

    @Test
    void monthlyTrendGroupsByCalendarMonthOldestFirst() {
        UUID donorOrgId = UUID.randomUUID();
        Instant earlierMonth = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant laterMonth = LocalDate.of(2026, 3, 20).atStartOfDay(ZoneOffset.UTC).toInstant();
        recordRescueAs(donorOrgId, "PRODUCE", new BigDecimal("2"), "KG", earlierMonth);
        recordRescueAs(donorOrgId, "PRODUCE", new BigDecimal("3"), "KG", laterMonth);

        TenantContext.set(tenantId);
        List<MonthlyImpactSummary> orgTrend = impactService.getOrgMonthlyTrend(donorOrgId);
        assertThat(orgTrend).hasSize(2);
        assertThat(orgTrend.get(0).month()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(orgTrend.get(0).totalKgSaved()).isEqualByComparingTo("2.000");
        assertThat(orgTrend.get(1).month()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(orgTrend.get(1).totalKgSaved()).isEqualByComparingTo("3.000");

        List<MonthlyImpactSummary> communityTrend = impactService.getCommunityMonthlyTrend();
        assertThat(communityTrend).hasSize(2);
    }

    private void recordRescueAs(UUID donorOrgId, String foodCategory, BigDecimal quantityValue, String quantityUnit, Instant completedAt) {
        UUID listingId = UUID.randomUUID();
        when(foodServiceClient.getFoodListing(eq(tenantId), eq(listingId)))
                .thenReturn(new FoodListingDto(listingId, donorOrgId, foodCategory, quantityValue, quantityUnit));

        impactService.recordFromPickupCompleted(new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, completedAt, "pickup-service",
                UUID.randomUUID(), listingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }
}
