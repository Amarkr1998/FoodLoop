package com.foodloop.ai.api;

import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.AgentRunStatus;
import com.foodloop.ai.domain.PendingNgoAllocation;
import com.foodloop.ai.domain.PendingNgoAllocationRepository;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.ai.tool.matching.CreateMatchProposalCommand;
import com.foodloop.ai.tool.matching.CreateMatchProposalTool;
import com.foodloop.commons.web.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The human-in-the-loop resolution point for every agent's escalation path
 * (spec §26) — currently only the NGO Coordination Agent's high-value
 * allocation gate (spec §19) has a pending action to resume on approval;
 * other agents' escalations (Rescue, Safety) are terminal by design and are
 * acted on through their own domain's existing human-review surface (e.g.
 * Food's safety-flag/clear), not this endpoint.
 */
@RestController
public class AgentRunController {

    private static final String AGENT_NAME_NGO_COORDINATION = "ngo-coordination";

    private final AgentRunRepository agentRunRepository;
    private final PendingNgoAllocationRepository pendingNgoAllocationRepository;
    private final ToolExecutor toolExecutor;
    private final CreateMatchProposalTool createMatchProposalTool;

    public AgentRunController(
            AgentRunRepository agentRunRepository,
            PendingNgoAllocationRepository pendingNgoAllocationRepository,
            ToolExecutor toolExecutor,
            CreateMatchProposalTool createMatchProposalTool) {
        this.agentRunRepository = agentRunRepository;
        this.pendingNgoAllocationRepository = pendingNgoAllocationRepository;
        this.toolExecutor = toolExecutor;
        this.createMatchProposalTool = createMatchProposalTool;
    }

    @PostMapping("/api/v1/ai/agent-runs/{id}/escalate/resolve")
    public AgentRunResponse resolve(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @Valid @RequestBody ResolveEscalationRequest request) {
        requireNgoOpsCaller(authentication);
        UUID reviewerUserId = callerUserId(authentication);

        AgentRun agentRun = agentRunRepository.findById(id)
                .orElseThrow(() -> new ApiException("AGENT_RUN_NOT_FOUND", HttpStatus.NOT_FOUND, "Agent run " + id + " was not found."));
        if (agentRun.getStatus() != AgentRunStatus.ESCALATED) {
            throw new ApiException("AGENT_RUN_NOT_ESCALATED", HttpStatus.CONFLICT,
                    "Agent run " + id + " is not in an escalated state (status=" + agentRun.getStatus() + ").");
        }
        if (!AGENT_NAME_NGO_COORDINATION.equals(agentRun.getAgentName())) {
            throw new ApiException("NO_RESUMABLE_ACTION", HttpStatus.CONFLICT,
                    "Agent run " + id + " (" + agentRun.getAgentName() + ") has no resumable action on this endpoint.");
        }
        PendingNgoAllocation pending = pendingNgoAllocationRepository.findByAgentRunId(id)
                .orElseThrow(() -> new ApiException("NO_RESUMABLE_ACTION", HttpStatus.CONFLICT,
                        "Agent run " + id + " has no pending allocation to resolve."));

        if (Boolean.TRUE.equals(request.approve())) {
            AgentCallerContext caller = new AgentCallerContext(AGENT_NAME_NGO_COORDINATION, agentRun.getTenantId(), agentRun.getId());
            var proposal = toolExecutor.run(createMatchProposalTool, caller, new CreateMatchProposalCommand(
                    pending.getFoodListingId(), pending.getNgoOrgId(),
                    "Approved by NGO ops (reviewer=" + reviewerUserId + ") after human-review escalation.",
                    pending.getNgoRequestId()));
            pending.approve(reviewerUserId);
            agentRun.complete("Approved by NGO ops; match proposal " + proposal.id() + " created.");
        } else {
            pending.reject(reviewerUserId);
            agentRun.complete("Rejected by NGO ops (reviewer=" + reviewerUserId + "); no proposal created.");
        }
        pendingNgoAllocationRepository.save(pending);
        return AgentRunResponse.from(agentRunRepository.save(agentRun));
    }

    /**
     * Reads Keycloak's standard {@code realm_access.roles} claim directly
     * off the JWT — same pattern as Food's {@code requireTrustOpsCaller}
     * (see that method's Javadoc for why there's no shared backend-commons
     * helper for this yet).
     */
    void requireNgoOpsCaller(JwtAuthenticationToken authentication) {
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof java.util.Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("NGO_OPS") && !roles.contains("ADMIN")) {
            throw new ApiException("FORBIDDEN_ESCALATION_RESOLVE", HttpStatus.FORBIDDEN,
                    "Only NGO_OPS or ADMIN may resolve an escalated agent run.");
        }
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
