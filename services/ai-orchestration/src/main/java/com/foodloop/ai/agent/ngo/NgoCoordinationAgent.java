package com.foodloop.ai.agent.ngo;

import com.foodloop.ai.client.FoodSearchResultDto;
import com.foodloop.ai.client.NgoRequirementDto;
import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.PendingNgoAllocation;
import com.foodloop.ai.domain.PendingNgoAllocationRepository;
import com.foodloop.ai.graph.AgentGraph;
import com.foodloop.ai.graph.GraphNode;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.ai.tool.matching.CreateMatchProposalCommand;
import com.foodloop.ai.tool.matching.CreateMatchProposalTool;
import com.foodloop.ai.tool.ngo.GetNgoRequestTool;
import com.foodloop.ai.tool.ngo.GetNgoRequirementsTool;
import com.foodloop.ai.tool.ngo.GetOrganizationTool;
import com.foodloop.ai.tool.ngo.SearchNearbyFoodInput;
import com.foodloop.ai.tool.ngo.SearchNearbyFoodTool;
import com.foodloop.commons.web.ApiException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fifth production agent: Observe -&gt; Retrieve (request + NGO org) -&gt; Find
 * Candidates -&gt; Decide (propose or escalate) (docs/architecture/05-ai-agent-architecture.md
 * §2, §19). Like Rescue, this agent makes no model call — matching a stated
 * bulk request against nearby available food by category/quantity/freshness
 * is fully mechanical, so there's nothing for an LLM to reason about.
 *
 * <p>Human approval gate (spec §19, §26): when the chosen candidate's
 * quantity exceeds {@code foodloop.ngo-coordination.escalation-quantity-threshold},
 * this agent does <em>not</em> call {@code createMatchProposal} itself —
 * it persists a {@link PendingNgoAllocation} and escalates the
 * {@link AgentRun}, and {@code createMatchProposal} only runs later, from
 * {@code POST /api/v1/ai/agent-runs/{id}/escalate/resolve} on approval. This
 * is the one action §5's permission table calls "schedulePickup" for this
 * agent — no separate claim-on-behalf-of-the-NGO tool exists: pickup
 * scheduling is Pickup's own automatic reaction to a claim (§4), and Food's
 * claim endpoint deliberately binds the claiming user to the caller's own
 * JWT (see FoodListingController#claim), not a service-supplied id — an
 * NGO's own authenticated member still has to accept the resulting
 * proposal, exactly like every other match proposal on this platform.
 */
@Component
public class NgoCoordinationAgent {

    private static final Logger log = LoggerFactory.getLogger(NgoCoordinationAgent.class);

    private static final String AGENT_NAME = "ngo-coordination";

    private final ToolExecutor toolExecutor;
    private final GetNgoRequestTool getNgoRequestTool;
    private final GetNgoRequirementsTool getNgoRequirementsTool;
    private final GetOrganizationTool getOrganizationTool;
    private final SearchNearbyFoodTool searchNearbyFoodTool;
    private final CreateMatchProposalTool createMatchProposalTool;
    private final AgentRunRepository agentRunRepository;
    private final PendingNgoAllocationRepository pendingNgoAllocationRepository;
    private final double searchRadiusKm;
    private final BigDecimal escalationQuantityThreshold;

    public NgoCoordinationAgent(
            ToolExecutor toolExecutor,
            GetNgoRequestTool getNgoRequestTool,
            GetNgoRequirementsTool getNgoRequirementsTool,
            GetOrganizationTool getOrganizationTool,
            SearchNearbyFoodTool searchNearbyFoodTool,
            CreateMatchProposalTool createMatchProposalTool,
            AgentRunRepository agentRunRepository,
            PendingNgoAllocationRepository pendingNgoAllocationRepository,
            @Value("${foodloop.ngo-coordination.search-radius-km:15}") double searchRadiusKm,
            @Value("${foodloop.ngo-coordination.escalation-quantity-threshold:50}") BigDecimal escalationQuantityThreshold) {
        this.toolExecutor = toolExecutor;
        this.getNgoRequestTool = getNgoRequestTool;
        this.getNgoRequirementsTool = getNgoRequirementsTool;
        this.getOrganizationTool = getOrganizationTool;
        this.searchNearbyFoodTool = searchNearbyFoodTool;
        this.createMatchProposalTool = createMatchProposalTool;
        this.agentRunRepository = agentRunRepository;
        this.pendingNgoAllocationRepository = pendingNgoAllocationRepository;
        this.searchRadiusKm = searchRadiusKm;
        this.escalationQuantityThreshold = escalationQuantityThreshold;
    }

    public record CoordinationResult(AgentRun agentRun) {
    }

    @Transactional
    public CoordinationResult coordinate(UUID tenantId, UUID ngoRequestId) {
        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, AGENT_NAME, ngoRequestId));
        AgentCallerContext caller = new AgentCallerContext(AGENT_NAME, tenantId, agentRun.getId());

        AgentGraph<NgoCoordinationState> graph = AgentGraph.<NgoCoordinationState>builder("loadRequest")
                .node(loadRequestNode(caller))
                .node(loadOrgNode(caller))
                .node(searchCandidatesNode(caller))
                .node(decideNode(caller))
                .edge("loadRequest", state -> state.requestNoLongerOpen() ? AgentGraph.END : "loadOrg")
                .edge("loadOrg", state -> "searchCandidates")
                .edge("searchCandidates", state -> state.chosenCandidate() == null ? AgentGraph.END : "decide")
                .edge("decide", state -> AgentGraph.END)
                .build();

        NgoCoordinationState finalState;
        try {
            finalState = graph.run(NgoCoordinationState.initial(ngoRequestId));
        } catch (RuntimeException e) {
            log.warn("NGO coordination agent run {} failed for request {}", agentRun.getId(), ngoRequestId, e);
            agentRun.fail("NGO coordination failed: " + e.getMessage());
            return new CoordinationResult(agentRunRepository.save(agentRun));
        }

        finalizeOutcome(agentRun, finalState);
        return new CoordinationResult(agentRunRepository.save(agentRun));
    }

    private void finalizeOutcome(AgentRun agentRun, NgoCoordinationState state) {
        if (state.requestNoLongerOpen()) {
            String status = state.request() != null ? state.request().status() : "unknown";
            agentRun.complete("NGO request " + state.ngoRequestId() + " is no longer OPEN (status=" + status + ").");
            return;
        }
        if (state.chosenCandidate() == null) {
            agentRun.complete("No eligible nearby food found for NGO request " + state.ngoRequestId()
                    + " within " + searchRadiusKm + "km.");
            return;
        }
        if (state.escalated()) {
            PendingNgoAllocation pending = new PendingNgoAllocation(
                    agentRun.getTenantId(), agentRun.getId(), state.ngoRequestId(), state.request().ngoOrgId(),
                    state.chosenCandidate().id(), state.request().quantityNeeded(), state.request().quantityUnit());
            pendingNgoAllocationRepository.save(pending);
            agentRun.escalate("Allocation of " + state.request().quantityNeeded() + " " + state.request().quantityUnit()
                    + " (listing " + state.chosenCandidate().id() + ") exceeds the "
                    + escalationQuantityThreshold + " auto-approval threshold — awaiting NGO-ops review.");
            return;
        }
        if (state.skippedAsDuplicate()) {
            agentRun.complete("NGO request " + state.ngoRequestId()
                    + " already has an open proposal for listing " + state.chosenCandidate().id() + "; nothing new to propose.");
            return;
        }
        agentRun.complete("Proposed listing " + state.proposal().id() + " to fulfill NGO request "
                + state.ngoRequestId() + " (matchProposalId=" + state.proposal().id() + ").");
    }

    private GraphNode<NgoCoordinationState> loadRequestNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "loadRequest";
            }

            @Override
            public NgoCoordinationState execute(NgoCoordinationState state) {
                var request = toolExecutor.run(getNgoRequestTool, caller, state.ngoRequestId());
                return state.withRequest(request);
            }
        };
    }

    private GraphNode<NgoCoordinationState> loadOrgNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "loadOrg";
            }

            @Override
            public NgoCoordinationState execute(NgoCoordinationState state) {
                var org = toolExecutor.run(getOrganizationTool, caller, state.request().ngoOrgId());
                return state.withNgoOrg(org);
            }
        };
    }

    private GraphNode<NgoCoordinationState> searchCandidatesNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "searchCandidates";
            }

            @Override
            public NgoCoordinationState execute(NgoCoordinationState state) {
                if (state.ngoOrg().latitude() == null || state.ngoOrg().longitude() == null) {
                    log.info("NGO org {} has no location set; NGO Coordination cannot search nearby food.",
                            state.request().ngoOrgId());
                    return state.withCandidates(List.of(), null);
                }
                NgoRequirementDto requirements = toolExecutor.run(getNgoRequirementsTool, caller, state.request().ngoOrgId());
                List<FoodSearchResultDto> results = toolExecutor.run(searchNearbyFoodTool, caller, new SearchNearbyFoodInput(
                        state.ngoOrg().latitude(), state.ngoOrg().longitude(), searchRadiusKm, state.request().foodCategory()));

                FoodSearchResultDto chosen = selectBestCandidate(results, state.request().quantityNeeded());
                return state.withCandidates(results, chosen);
            }
        };
    }

    /** Deterministic (spec §19): the smallest listing that still meets the requested quantity, tie-broken by soonest expiry — never the LLM's choice. */
    private FoodSearchResultDto selectBestCandidate(List<FoodSearchResultDto> candidates, BigDecimal quantityNeeded) {
        return candidates.stream()
                .filter(c -> c.quantityValue() != null && c.quantityValue().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator
                        .<FoodSearchResultDto>comparingInt(c -> c.quantityValue().compareTo(quantityNeeded) >= 0 ? 0 : 1)
                        .thenComparing(FoodSearchResultDto::expiryTime))
                .orElse(null);
    }

    private GraphNode<NgoCoordinationState> decideNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "decide";
            }

            @Override
            public NgoCoordinationState execute(NgoCoordinationState state) {
                if (state.request().quantityNeeded().compareTo(escalationQuantityThreshold) > 0) {
                    return state.withEscalated();
                }
                try {
                    var proposal = toolExecutor.run(createMatchProposalTool, caller, new CreateMatchProposalCommand(
                            state.chosenCandidate().id(), state.request().ngoOrgId(),
                            "Automated NGO Coordination allocation: fulfills open request for "
                                    + state.request().quantityNeeded() + " " + state.request().quantityUnit()
                                    + " of " + state.request().foodCategory() + ". Deterministic rationale — no model call was made.",
                            state.ngoRequestId()));
                    return state.withProposal(proposal);
                } catch (ApiException e) {
                    // A prior sweep tick (or the async match.proposed.v1 consumer racing
                    // this one) may have already proposed this same pair — same
                    // MATCH_ALREADY_PROPOSED guard RescueAgent's notifyAndProposeNode
                    // handles the same way. Not a coordination failure, just nothing new
                    // to propose this tick.
                    log.info("NGO Coordination skipped creating a duplicate match proposal for request {} / listing {}: {}",
                            state.ngoRequestId(), state.chosenCandidate().id(), e.getMessage());
                    return state.withSkippedAsDuplicate();
                }
            }
        };
    }
}
