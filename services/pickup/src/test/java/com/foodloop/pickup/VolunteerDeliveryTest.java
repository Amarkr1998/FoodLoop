package com.foodloop.pickup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.application.PickupService;
import com.foodloop.pickup.application.VolunteerService;
import com.foodloop.pickup.domain.PickupTask;
import com.foodloop.pickup.domain.PickupTaskRepository;
import com.foodloop.pickup.domain.VehicleType;
import com.foodloop.pickup.domain.VolunteerProfile;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the volunteer-mediated workflow end-to-end (spec Phase 10): a
 * registered volunteer can find, claim, and progress a task through to
 * completion, the assigned-volunteer check is real (not just the donor's
 * own authorization), and a second registration for the same person is
 * rejected rather than silently creating a duplicate profile.
 */
@SpringBootTest
@Testcontainers
class VolunteerDeliveryTest {

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
    private VolunteerService volunteerService;

    @Autowired
    private PickupTaskRepository pickupTaskRepository;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void volunteerClaimsProgressesAndCompletesAPickupTask() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.BICYCLE, 10);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());

        PickupTask claimed = pickupService.claimAsVolunteer(task.getId(), volunteerUserId);
        assertThat(claimed.getStatus().name()).isEqualTo("ASSIGNED");
        assertThat(claimed.getAssignedVolunteerId()).isEqualTo(volunteerUserId);

        pickupService.volunteerEnRoute(task.getId(), volunteerUserId);
        PickupTask arrived = pickupService.volunteerArrived(task.getId(), volunteerUserId);
        assertThat(arrived.getStatus().name()).isEqualTo("ARRIVED");

        // The assigned volunteer, not just the donor, may confirm completion.
        PickupTask completed = pickupService.complete(task.getId(), volunteerUserId);
        assertThat(completed.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void onlyTheAssignedVolunteerCanMarkEnRoute() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        UUID someoneElseId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.CAR, 20);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());
        pickupService.claimAsVolunteer(task.getId(), volunteerUserId);

        assertThatThrownBy(() -> pickupService.volunteerEnRoute(task.getId(), someoneElseId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void aVolunteerCanBackOutAndTheTaskReturnsToTheOpenPool() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.ON_FOOT, 5);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());
        pickupService.claimAsVolunteer(task.getId(), volunteerUserId);

        PickupTask backOut = pickupService.unassignVolunteer(task.getId(), volunteerUserId);
        assertThat(backOut.getStatus().name()).isEqualTo("UNASSIGNED");
        assertThat(backOut.getAssignedVolunteerId()).isNull();
    }

    @Test
    void registeringTwiceForTheSamePersonIsRejected() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.SCOOTER, 8);

        assertThatThrownBy(() -> volunteerService.register(tenantId, volunteerUserId, VehicleType.CAR, 20))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void volunteersCanFindNearbyUnassignedTasks() {
        TenantContext.set(tenantId);
        PickupTask nearTask = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(nearTask.getId(), nearTask.getDonorUserId());
        PickupTask farTask = createScheduledTask(40.7128, -74.0060); // New York — nowhere near Bangalore
        pickupService.requestVolunteer(farTask.getId(), farTask.getDonorUserId());

        var results = pickupService.searchAvailableForVolunteers(tenantId, 12.9716, 77.5946, 10.0, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(PickupTask::getId).containsExactly(nearTask.getId());
    }

    private PickupTask createScheduledTask(double lat, double lng) {
        Instant now = Instant.now();
        FoodClaimedEvent event = new FoodClaimedEvent(
                UUID.randomUUID(), "FOOD_CLAIMED", 1, tenantId, now, "food-service",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), lat, lng);
        pickupService.createFromClaim(event);
        TenantContext.set(tenantId);
        return pickupTaskRepository.findByClaimId(event.claimId()).orElseThrow();
    }
}
