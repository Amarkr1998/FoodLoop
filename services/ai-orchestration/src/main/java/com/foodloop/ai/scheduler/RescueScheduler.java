package com.foodloop.ai.scheduler;

import com.foodloop.ai.agent.rescue.RescueAgent;
import com.foodloop.ai.agent.rescue.RescueThreshold;
import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.TenantDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.commons.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The Food Rescue Agent's trigger (spec §18: "scheduled job emitting
 * food.expiring.v1 at configurable thresholds"). No Kafka topic is actually
 * published — this drives {@link RescueAgent} directly instead — since
 * nothing else in the platform would consume such a topic yet, and
 * publishing one with no subscriber would be exactly the empty scaffolding
 * spec §63 forbids.
 *
 * <p>Iterates every active tenant (there is no per-tenant HTTP trigger for
 * this, unlike the other agents — a background sweep has no inbound request
 * to derive a tenant from) and, per tenant, every AVAILABLE listing expiring
 * within the wider threshold, bucketing each into T-4h or T-1h by its actual
 * remaining time. {@link AgentRunRepository} doubles as the idempotency
 * store: a listing already handled at a given threshold (its outcome
 * summary carries that threshold's tag) is skipped, so a 5-minute sweep
 * interval doesn't re-notify the same receivers repeatedly.
 */
@Component
public class RescueScheduler {

    private static final Logger log = LoggerFactory.getLogger(RescueScheduler.class);
    private static final String AGENT_NAME = "rescue";

    private final TenantServiceClient tenantServiceClient;
    private final FoodServiceClient foodServiceClient;
    private final AgentRunRepository agentRunRepository;
    private final RescueAgent rescueAgent;
    private final int thresholdMinutes4h;
    private final int thresholdMinutes1h;

    public RescueScheduler(
            TenantServiceClient tenantServiceClient,
            FoodServiceClient foodServiceClient,
            AgentRunRepository agentRunRepository,
            RescueAgent rescueAgent,
            @Value("${foodloop.rescue.threshold-minutes-4h:240}") int thresholdMinutes4h,
            @Value("${foodloop.rescue.threshold-minutes-1h:60}") int thresholdMinutes1h) {
        this.tenantServiceClient = tenantServiceClient;
        this.foodServiceClient = foodServiceClient;
        this.agentRunRepository = agentRunRepository;
        this.rescueAgent = rescueAgent;
        this.thresholdMinutes4h = thresholdMinutes4h;
        this.thresholdMinutes1h = thresholdMinutes1h;
    }

    // initialDelayString matters: fixedDelayString alone fires the first tick
    // almost immediately after context startup, which would race every
    // @SpringBootTest in this module (including ones that don't mock these
    // clients at all) against a background sweep mid-context-refresh. 30
    // minutes safely exceeds this environment's slowest observed test
    // startup (~6 minutes) without needing every other test to opt out.
    @Scheduled(
            fixedDelayString = "${foodloop.rescue.sweep-interval-ms:300000}",
            initialDelayString = "${foodloop.rescue.initial-delay-ms:1800000}")
    public void sweep() {
        List<TenantDto> tenants;
        try {
            tenants = tenantServiceClient.listActiveTenants();
        } catch (RuntimeException e) {
            log.warn("Rescue sweep could not list active tenants; skipping this tick", e);
            return;
        }
        for (TenantDto tenant : tenants) {
            sweepTenant(tenant.id());
        }
    }

    private void sweepTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            List<FoodListingDto> expiring = foodServiceClient.getExpiringListings(tenantId, thresholdMinutes4h);
            Instant now = Instant.now();
            for (FoodListingDto listing : expiring) {
                long minutesRemaining = Duration.between(now, listing.expiryTime()).toMinutes();
                RescueThreshold threshold = minutesRemaining <= thresholdMinutes1h
                        ? RescueThreshold.T_MINUS_1H
                        : RescueThreshold.T_MINUS_4H;
                if (alreadyHandled(listing.id(), threshold)) {
                    continue;
                }
                rescueAgent.check(tenantId, listing.id(), threshold);
            }
        } catch (RuntimeException e) {
            log.warn("Rescue sweep failed for tenant {}", tenantId, e);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean alreadyHandled(UUID listingId, RescueThreshold threshold) {
        String tag = "[" + threshold.name() + "]";
        return agentRunRepository.findByAgentNameAndTriggerEventId(AGENT_NAME, listingId).stream()
                .anyMatch(run -> run.getOutcomeSummary() != null && run.getOutcomeSummary().contains(tag));
    }
}
