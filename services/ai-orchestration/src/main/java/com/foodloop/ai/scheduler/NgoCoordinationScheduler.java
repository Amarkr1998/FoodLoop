package com.foodloop.ai.scheduler;

import com.foodloop.ai.agent.ngo.NgoCoordinationAgent;
import com.foodloop.ai.client.NgoRequestDto;
import com.foodloop.ai.client.NgoServiceClient;
import com.foodloop.ai.client.TenantDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.AgentRunStatus;
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
 * The NGO Coordination Agent's trigger (spec §19: "ngo.request.created.v1
 * or scheduled sweep for open requests nearing needed_before"). Unlike
 * Rescue, no on-demand HTTP trigger exists yet either — every request is
 * caught by this sweep instead, which is simpler and sufficient given a
 * bulk request's own {@code neededBefore} deadline is typically hours-to-days
 * out, not minutes (RescueScheduler's threshold-bucket precedent doesn't
 * apply here). {@link AgentRunRepository} doubles as the idempotency store,
 * same precedent as RescueScheduler: a request with an unresolved escalated
 * run is skipped until a human resolves it, so a repeated sweep tick never
 * creates a second competing escalation for the same request.
 */
@Component
public class NgoCoordinationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NgoCoordinationScheduler.class);
    private static final String AGENT_NAME = "ngo-coordination";

    private final TenantServiceClient tenantServiceClient;
    private final NgoServiceClient ngoServiceClient;
    private final AgentRunRepository agentRunRepository;
    private final NgoCoordinationAgent ngoCoordinationAgent;
    private final int lookaheadHours;

    public NgoCoordinationScheduler(
            TenantServiceClient tenantServiceClient,
            NgoServiceClient ngoServiceClient,
            AgentRunRepository agentRunRepository,
            NgoCoordinationAgent ngoCoordinationAgent,
            @Value("${foodloop.ngo-coordination.lookahead-hours:72}") int lookaheadHours) {
        this.tenantServiceClient = tenantServiceClient;
        this.ngoServiceClient = ngoServiceClient;
        this.agentRunRepository = agentRunRepository;
        this.ngoCoordinationAgent = ngoCoordinationAgent;
        this.lookaheadHours = lookaheadHours;
    }

    // See RescueScheduler's identical initialDelayString Javadoc — same
    // reason: avoid racing this module's @SpringBootTest context startup.
    @Scheduled(
            fixedDelayString = "${foodloop.ngo-coordination.sweep-interval-ms:300000}",
            initialDelayString = "${foodloop.ngo-coordination.initial-delay-ms:1800000}")
    public void sweep() {
        List<TenantDto> tenants;
        try {
            tenants = tenantServiceClient.listActiveTenants();
        } catch (RuntimeException e) {
            log.warn("NGO coordination sweep could not list active tenants; skipping this tick", e);
            return;
        }
        for (TenantDto tenant : tenants) {
            sweepTenant(tenant.id());
        }
    }

    private void sweepTenant(UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            Instant cutoff = Instant.now().plus(Duration.ofHours(lookaheadHours));
            List<NgoRequestDto> openRequests = ngoServiceClient.listOpenRequestsNearingDeadline(tenantId, cutoff);
            for (NgoRequestDto request : openRequests) {
                if (hasUnresolvedEscalation(request.id())) {
                    continue;
                }
                ngoCoordinationAgent.coordinate(tenantId, request.id());
            }
        } catch (RuntimeException e) {
            log.warn("NGO coordination sweep failed for tenant {}", tenantId, e);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean hasUnresolvedEscalation(UUID ngoRequestId) {
        return agentRunRepository.findByAgentNameAndTriggerEventId(AGENT_NAME, ngoRequestId).stream()
                .anyMatch(run -> run.getStatus() == AgentRunStatus.ESCALATED);
    }
}
