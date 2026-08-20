package com.foodloop.pickup;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
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

/** The server-side support the Pickup Agent (spec §20) relies on: the delayed-task sweep, system-initiated reassignment, and reverse volunteer search. */
@SpringBootTest
@Testcontainers
class PickupAgentSupportTest {

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
    void findDelayedIncludesAnAssignedTaskOnceItsWindowHasPassedButExcludesOneStillWithinWindow() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.BICYCLE, 10);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());
        pickupService.claimAsVolunteer(task.getId(), volunteerUserId);

        var notYetDelayed = pickupService.findDelayed(tenantId, Instant.now());
        assertThat(notYetDelayed).extracting(PickupTask::getId).doesNotContain(task.getId());

        var delayed = pickupService.findDelayed(tenantId, Instant.now().plus(4, ChronoUnit.HOURS));
        assertThat(delayed).extracting(PickupTask::getId).contains(task.getId());
    }

    @Test
    void findDelayedExcludesTasksThatAlreadyArrived() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.CAR, 20);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());
        pickupService.claimAsVolunteer(task.getId(), volunteerUserId);
        pickupService.volunteerEnRoute(task.getId(), volunteerUserId);
        pickupService.volunteerArrived(task.getId(), volunteerUserId);

        var delayed = pickupService.findDelayed(tenantId, Instant.now().plus(4, ChronoUnit.HOURS));
        assertThat(delayed).extracting(PickupTask::getId).doesNotContain(task.getId());
    }

    @Test
    void systemUnassignFreesTheTaskWithoutAnyCallerIdentity() {
        TenantContext.set(tenantId);
        UUID volunteerUserId = UUID.randomUUID();
        volunteerService.register(tenantId, volunteerUserId, VehicleType.ON_FOOT, 5);

        PickupTask task = createScheduledTask(12.9716, 77.5946);
        pickupService.requestVolunteer(task.getId(), task.getDonorUserId());
        pickupService.claimAsVolunteer(task.getId(), volunteerUserId);

        PickupTask freed = pickupService.systemUnassignVolunteer(task.getId());
        assertThat(freed.getStatus().name()).isEqualTo("UNASSIGNED");
        assertThat(freed.getAssignedVolunteerId()).isNull();
    }

    @Test
    void findNearbyAvailableVolunteersFindsAnAvailableOneButNotAnUnavailableOrFarOne() {
        TenantContext.set(tenantId);
        UUID availableNearby = UUID.randomUUID();
        UUID unavailableNearby = UUID.randomUUID();
        UUID availableFar = UUID.randomUUID();
        VolunteerProfile v1 = volunteerService.register(tenantId, availableNearby, VehicleType.BICYCLE, 10);
        volunteerService.updateLocation(availableNearby, 12.9720, 77.5950);
        VolunteerProfile v2 = volunteerService.register(tenantId, unavailableNearby, VehicleType.CAR, 20);
        volunteerService.updateLocation(unavailableNearby, 12.9720, 77.5950);
        volunteerService.updateAvailability(unavailableNearby, false);
        volunteerService.register(tenantId, availableFar, VehicleType.CAR, 20);
        volunteerService.updateLocation(availableFar, 40.7128, -74.0060);

        PickupTask task = createScheduledTask(12.9716, 77.5946);

        var results = pickupService.findNearbyAvailableVolunteers(task.getId(), 10.0, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(VolunteerProfile::getUserId).containsExactly(availableNearby);
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
