package com.foodloop.ai.scheduler;

import com.foodloop.ai.agent.pickup.PickupAgent;
import com.foodloop.ai.client.PickupServiceClient;
import com.foodloop.ai.client.PickupTaskDto;
import com.foodloop.ai.client.TenantDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.commons.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The Pickup Agent's trigger (spec §20: detecting a delay "via a
 * deterministic timer against scheduled_window"). Same structure as
 * RescueScheduler/NgoCoordinationScheduler: iterates every active tenant,
 * sweeps its delayed tasks, and uses {@link AgentRunRepository} as the
 * idempotency store so a repeated tick doesn't re-notify/re-unassign a task
 * this sweep already handled this tick.
 */
@Component
public class PickupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PickupScheduler.class);
    private static final String AGENT_NAME = "pickup";

    private final TenantServiceClient tenantServiceClient;
    private final PickupServiceClient pickupServiceClient;
    private final AgentRunRepository agentRunRepository;
    private final PickupAgent pickupAgent;

    public PickupScheduler(
            TenantServiceClient tenantServiceClient,
            PickupServiceClient pickupServiceClient,
            AgentRunRepository agentRunRepository,
            PickupAgent pickupAgent) {
        this.tenantServiceClient = tenantServiceClient;
        this.pickupServiceClient = pickupServiceClient;
        this.agentRunRepository = agentRunRepository;
        this.pickupAgent = pickupAgent;
    }

    // See RescueScheduler's identical initialDelayString Javadoc — same
    // reason: avoid racing this module's @SpringBootTest context startup.
    @Scheduled(
            fixedDelayString = "${foodloop.pickup.sweep-interval-ms:300000}",
            initialDelayString = "${foodloop.pickup.initial-delay-ms:1800000}")
    public void sweep() {
        List<TenantDto> tenants;
        try {
            tenants = tenantServiceClient.listActiveTenants();
        } catch (RuntimeException e) {
            log.warn("Pickup sweep could not list active tenants; skipping this tick", e);
            return;
        }
        for (TenantDto tenant : tenants) {
            sweepTenant(tenant.id());
        }
    }

    private void sweepTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            List<PickupTaskDto> delayed = pickupServiceClient.findDelayed(tenantId, Instant.now());
            for (PickupTaskDto task : delayed) {
                if (alreadyHandledThisTick(task.id())) {
                    continue;
                }
                pickupAgent.checkDelay(tenantId, task.id());
            }
        } catch (RuntimeException e) {
            log.warn("Pickup sweep failed for tenant {}", tenantId, e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Once this sweep frees a task (system-unassign), a later tick would
     * see it back in ASSIGNED after a new volunteer claims it and its
     * scheduled window is still in the past — that's a genuinely new delay
     * worth re-checking, not a duplicate, so only an unresolved run from
     * <em>this exact sweep pass</em> would double-handle it. In practice a
     * single sweepTenant() call only ever visits a given taskId once per
     * tick, so no in-tick dedupe is needed beyond what the loop already
     * guarantees; this check guards the rarer case of two overlapping
     * sweep ticks (a slow previous tick still running when the next fires).
     */
    private boolean alreadyHandledThisTick(UUID pickupTaskId) {
        return agentRunRepository.findByAgentNameAndTriggerEventId(AGENT_NAME, pickupTaskId).stream()
                .anyMatch(run -> run.getStartedAt().isAfter(Instant.now().minusSeconds(60)));
    }
}
