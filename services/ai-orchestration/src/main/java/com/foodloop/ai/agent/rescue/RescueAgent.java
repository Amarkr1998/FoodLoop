package com.foodloop.ai.agent.rescue;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.graph.AgentGraph;
import com.foodloop.ai.graph.GraphNode;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.ai.tool.food.GetFoodListingTool;
import com.foodloop.ai.tool.matching.CreateMatchProposalCommand;
import com.foodloop.ai.tool.matching.CreateMatchProposalTool;
import com.foodloop.ai.tool.matching.SearchNearbyReceiversInput;
import com.foodloop.ai.tool.matching.SearchNearbyReceiversTool;
import com.foodloop.ai.tool.rescue.SendNotificationCommand;
import com.foodloop.ai.tool.rescue.SendNotificationTool;
import com.foodloop.commons.web.ApiException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Third production agent (Phase 8): Observe -&gt; Retrieve -&gt; Find Receivers
 * -&gt; Notify/Propose -&gt; Continue/Escalate
 * (docs/architecture/05-ai-agent-architecture.md §2, §18). Unlike Food
 * Intelligence and Matching, Rescue makes no model call at all — the spec's
 * own workflow description for this agent ("monitor, detect risk, find
 * receivers, rank, notify, expand radius, escalate") is fully mechanical,
 * so there is nothing for an LLM to reason about; the graph engine doesn't
 * require every node to be model-backed.
 *
 * <p>{@link RescueThreshold#T_MINUS_1H} is always a terminal escalation,
 * whether or not notification/proposal succeeded — it's the "expand
 * radius, then escalate" tier from the spec, so human ops needs visibility
 * regardless of automated outcome once the deadline is this close.
 */
@Component
public class RescueAgent {

    private static final Logger log = LoggerFactory.getLogger(RescueAgent.class);

    private static final String AGENT_NAME = "rescue";

    private final ToolExecutor toolExecutor;
    private final GetFoodListingTool getFoodListingTool;
    private final SearchNearbyReceiversTool searchNearbyReceiversTool;
    private final SendNotificationTool sendNotificationTool;
    private final CreateMatchProposalTool createMatchProposalTool;
    private final AgentRunRepository agentRunRepository;
    private final int notifyTopN;
    private final double radiusKmAt4h;
    private final double radiusKmAt1h;

    public RescueAgent(
            ToolExecutor toolExecutor,
            GetFoodListingTool getFoodListingTool,
            SearchNearbyReceiversTool searchNearbyReceiversTool,
            SendNotificationTool sendNotificationTool,
            CreateMatchProposalTool createMatchProposalTool,
            AgentRunRepository agentRunRepository,
            @Value("${foodloop.rescue.notify-top-n:3}") int notifyTopN,
            @Value("${foodloop.rescue.radius-km-4h:10}") double radiusKmAt4h,
            @Value("${foodloop.rescue.radius-km-1h:25}") double radiusKmAt1h) {
        this.toolExecutor = toolExecutor;
        this.getFoodListingTool = getFoodListingTool;
        this.searchNearbyReceiversTool = searchNearbyReceiversTool;
        this.sendNotificationTool = sendNotificationTool;
        this.createMatchProposalTool = createMatchProposalTool;
        this.agentRunRepository = agentRunRepository;
        this.notifyTopN = notifyTopN;
        this.radiusKmAt4h = radiusKmAt4h;
        this.radiusKmAt1h = radiusKmAt1h;
    }

    public record RescueResult(AgentRun agentRun) {
    }

    public RescueResult check(UUID tenantId, UUID listingId, RescueThreshold threshold) {
        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, AGENT_NAME, listingId));
        AgentCallerContext caller = new AgentCallerContext(AGENT_NAME, tenantId, agentRun.getId());
        double radiusKm = threshold == RescueThreshold.T_MINUS_1H ? radiusKmAt1h : radiusKmAt4h;

        AgentGraph<RescueState> graph = AgentGraph.<RescueState>builder("retrieve")
                .node(retrieveNode(caller))
                .node(findReceiversNode(caller, radiusKm))
                .node(notifyAndProposeNode(caller, agentRun.getId()))
                .edge("retrieve", state -> state.listingNoLongerAvailable() ? AgentGraph.END : "findReceivers")
                .edge("findReceivers", state -> state.candidates().isEmpty() ? AgentGraph.END : "notifyAndPropose")
                .edge("notifyAndPropose", state -> AgentGraph.END)
                .build();

        RescueState finalState;
        try {
            finalState = graph.run(RescueState.initial(listingId, threshold));
        } catch (RuntimeException e) {
            log.warn("Rescue agent run {} failed for listing {}", agentRun.getId(), listingId, e);
            agentRun.fail("Rescue check failed: " + e.getMessage());
            return new RescueResult(agentRunRepository.save(agentRun));
        }

        finalizeOutcome(agentRun, finalState);
        return new RescueResult(agentRunRepository.save(agentRun));
    }

    private void finalizeOutcome(AgentRun agentRun, RescueState state) {
        String thresholdLabel = state.threshold().name();

        if (state.listingNoLongerAvailable()) {
            String status = state.listing() != null ? state.listing().status() : "unknown";
            agentRun.complete("[" + thresholdLabel + "] Listing " + state.listingId()
                    + " no longer needs rescue (status=" + status + ").");
            return;
        }

        boolean isFinalWindow = state.threshold() == RescueThreshold.T_MINUS_1H;
        if (state.candidates() == null || state.candidates().isEmpty()) {
            String summary = "[" + thresholdLabel + "] No eligible receivers found within " + radiusForLog(state)
                    + "km for listing " + state.listingId() + ".";
            if (isFinalWindow) {
                agentRun.escalate(summary + " Escalating to human ops — final rescue window.");
            } else {
                agentRun.complete(summary);
            }
            return;
        }

        String summary = "[" + thresholdLabel + "] Notified " + state.notifiedCount() + " nearby receiver(s)"
                + (state.proposedOrgId() != null ? "; proposed match with org " + state.proposedOrgId() : "") + ".";
        if (isFinalWindow) {
            agentRun.escalate(summary + " Flagged for human ops review — final rescue window.");
        } else {
            agentRun.complete(summary);
        }
    }

    private double radiusForLog(RescueState state) {
        return state.threshold() == RescueThreshold.T_MINUS_1H ? radiusKmAt1h : radiusKmAt4h;
    }

    private GraphNode<RescueState> retrieveNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "retrieve";
            }

            @Override
            public RescueState execute(RescueState state) {
                FoodListingDto listing = toolExecutor.run(getFoodListingTool, caller, state.listingId());
                return state.withListing(listing);
            }
        };
    }

    private GraphNode<RescueState> findReceiversNode(AgentCallerContext caller, double radiusKm) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "findReceivers";
            }

            @Override
            public RescueState execute(RescueState state) {
                var candidates = toolExecutor.run(searchNearbyReceiversTool, caller,
                        new SearchNearbyReceiversInput(state.listingId(), radiusKm));
                return state.withCandidates(candidates);
            }
        };
    }

    private GraphNode<RescueState> notifyAndProposeNode(AgentCallerContext caller, UUID agentRunId) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "notifyAndPropose";
            }

            @Override
            public RescueState execute(RescueState state) {
                int notified = 0;
                for (MatchCandidateDto candidate : state.candidates().stream().limit(notifyTopN).toList()) {
                    toolExecutor.run(sendNotificationTool, caller, new SendNotificationCommand(
                            candidate.receiverOrgId(), "IN_APP", "Food expiring soon nearby",
                            "A listing (\"" + state.listing().title() + "\") is expiring soon and is "
                                    + Math.round(candidate.distanceMeters()) + "m away. Consider claiming it.",
                            agentRunId));
                    notified++;
                }

                MatchCandidateDto top = state.candidates().get(0);
                UUID proposedOrgId = null;
                try {
                    var proposal = toolExecutor.run(createMatchProposalTool, caller, new CreateMatchProposalCommand(
                            state.listingId(), top.receiverOrgId(),
                            "Automated rescue proposal (" + state.threshold() + "): closest eligible receiver "
                                    + "for a listing nearing expiry. Deterministic rationale — no model call was made."));
                    proposedOrgId = proposal.receiverOrgId();
                } catch (ApiException e) {
                    // An earlier threshold's tick (or a concurrent trigger) may have already
                    // proposed this same pair — MatchingService's own idempotency guard
                    // (MATCH_ALREADY_PROPOSED) rejected it. Notification still went out above;
                    // this isn't a rescue failure, just nothing new to propose.
                    log.info("Rescue skipped creating a duplicate match proposal for listing {} / org {}: {}",
                            state.listingId(), top.receiverOrgId(), e.getMessage());
                }

                return state.withNotifiedCount(notified).withProposedOrgId(proposedOrgId);
            }
        };
    }
}
