package com.foodloop.food;

import static org.assertj.core.api.Assertions.assertThat;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.food.application.ClaimService;
import com.foodloop.food.application.FoodListingService;
import com.foodloop.food.domain.ClaimRepository;
import com.foodloop.food.domain.FoodListing;
import com.foodloop.food.domain.FoodListingRepository;
import com.foodloop.food.domain.FoodStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Spec §46: "Test critical race conditions such as two users claiming the
 * same food simultaneously." Two real threads, each with their own
 * transaction, race to claim the exact same AVAILABLE listing — exactly one
 * must win; the loser must get a clean 409, not a corrupted row or a
 * silently-accepted double claim. Repeated several times since a race
 * that's only sometimes exercised is not proven.
 */
@SpringBootTest
@Testcontainers
class ClaimRaceConditionTest {

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
        // Two real concurrent transactions need two real pooled connections.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    }

    @Autowired
    private FoodListingService foodListingService;

    @Autowired
    private ClaimService claimService;

    @Autowired
    private FoodListingRepository foodListingRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @RepeatedTest(5)
    void exactlyOneOfTwoSimultaneousClaimsSucceeds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID listingId = publishedListing(tenantId);

        UUID receiverA = UUID.randomUUID();
        UUID receiverB = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> attemptClaim(tenantId, listingId, receiverA, ready, go, successCount, conflictCount)),
                    executor.submit(() -> attemptClaim(tenantId, listingId, receiverB, ready, go, successCount, conflictCount)));

            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            for (Future<?> future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        TenantContext.set(tenantId);
        try {
            FoodListing listing = foodListingRepository.findById(listingId).orElseThrow();
            assertThat(listing.getStatus()).isEqualTo(FoodStatus.CLAIMED);
            assertThat(claimRepository.findAll()).hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }

    private void attemptClaim(
            UUID tenantId, UUID listingId, UUID receiverUserId,
            CountDownLatch ready, CountDownLatch go, AtomicInteger successCount, AtomicInteger conflictCount) {
        TenantContext.set(tenantId);
        try {
            ready.countDown();
            go.await(10, TimeUnit.SECONDS);
            claimService.claim(listingId, receiverUserId, null, "idem-" + receiverUserId);
            successCount.incrementAndGet();
        } catch (ApiException e) {
            if ("FOOD_ALREADY_CLAIMED".equals(e.getCode())) {
                conflictCount.incrementAndGet();
            } else {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID publishedListing(UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            FoodListing draft = foodListingService.createDraft(tenantId, UUID.randomUUID(), FoodTestSupport.sampleRequest(UUID.randomUUID()));
            FoodListing published = foodListingService.publish(draft.getId(), draft.getDonorUserId());
            return published.getId();
        } finally {
            TenantContext.clear();
        }
    }
}
