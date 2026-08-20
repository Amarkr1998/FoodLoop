package com.foodloop.ai.agent.pickup;

import com.foodloop.ai.client.VolunteerProfileDto;
import com.foodloop.ai.domain.AgentRun;
import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.graph.AgentGraph;
import com.foodloop.ai.graph.GraphNode;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.ai.tool.pickup.CalculateRouteInput;
import com.foodloop.ai.tool.pickup.CalculateRouteTool;
import com.foodloop.ai.tool.pickup.FindAvailableVolunteersInput;
import com.foodloop.ai.tool.pickup.FindAvailableVolunteersTool;
import com.foodloop.ai.tool.pickup.GetPickupTaskTool;
import com.foodloop.ai.tool.pickup.NotifyVolunteerCommand;
import com.foodloop.ai.tool.pickup.NotifyVolunteerTool;
import com.foodloop.ai.tool.pickup.RouteResult;
import com.foodloop.ai.tool.pickup.UpdatePickupStatusCommand;
import com.foodloop.ai.tool.pickup.UpdatePickupStatusTool;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Seventh production agent, and the last of the three agents deferred at
 * their original phase gates (Phase 10's volunteer delivery — see
 * VolunteerDeliveryTest's precedent comment on PickupService#claimAsVolunteer):
 * Observe -&gt; Retrieve task -&gt; Find Available Volunteers -&gt; Notify/Reassign
 * -&gt; Continue/Escalate (docs/architecture/05-ai-agent-architecture.md §2,
 * §20). Like Rescue, this agent makes no model call — detecting a delay
 * against {@code scheduled_window} and picking the nearest available
 * volunteer is fully mechanical.
 *
 * <p>{@code calculateRoute} (spec §20: "not computing ETAs" as the agent's
 * own job) is used only to attach a distance/ETA estimate to the
 * notification text — {@link com.foodloop.ai.tool.pickup.RouteCalculator}
 * itself, not this agent, does that arithmetic. Reassignment
 * (system-unassigning the unresponsive volunteer) is a fully automated,
 * reversible action here, not an escalation-gated one — see
 * {@code PickupService#systemUnassignVolunteer}'s Javadoc for why.
 */
@Component
public class PickupAgent {

    private static final Logger log = LoggerFactory.getLogger(PickupAgent.class);

    private static final String AGENT_NAME = "pickup";

    private final ToolExecutor toolExecutor;
    private final GetPickupTaskTool getPickupTaskTool;
    private final FindAvailableVolunteersTool findAvailableVolunteersTool;
    private final CalculateRouteTool calculateRouteTool;
    private final NotifyVolunteerTool notifyVolunteerTool;
    private final UpdatePickupStatusTool updatePickupStatusTool;
    private final AgentRunRepository agentRunRepository;
    private final double searchRadiusKm;
    private final int notifyTopN;

    public PickupAgent(
            ToolExecutor toolExecutor,
            GetPickupTaskTool getPickupTaskTool,
            FindAvailableVolunteersTool findAvailableVolunteersTool,
            CalculateRouteTool calculateRouteTool,
            NotifyVolunteerTool notifyVolunteerTool,
            UpdatePickupStatusTool updatePickupStatusTool,
            AgentRunRepository agentRunRepository,
            @Value("${foodloop.pickup.search-radius-km:10}") double searchRadiusKm,
            @Value("${foodloop.pickup.notify-top-n:3}") int notifyTopN) {
        this.toolExecutor = toolExecutor;
        this.getPickupTaskTool = getPickupTaskTool;
        this.findAvailableVolunteersTool = findAvailableVolunteersTool;
        this.calculateRouteTool = calculateRouteTool;
        this.notifyVolunteerTool = notifyVolunteerTool;
        this.updatePickupStatusTool = updatePickupStatusTool;
        this.agentRunRepository = agentRunRepository;
        this.searchRadiusKm = searchRadiusKm;
        this.notifyTopN = notifyTopN;
    }

    public record DelayCheckResult(AgentRun agentRun) {
    }

    public DelayCheckResult checkDelay(UUID tenantId, UUID pickupTaskId) {
        AgentRun agentRun = agentRunRepository.save(new AgentRun(tenantId, AGENT_NAME, pickupTaskId));
        AgentCallerContext caller = new AgentCallerContext(AGENT_NAME, tenantId, agentRun.getId());

        AgentGraph<PickupState> graph = AgentGraph.<PickupState>builder("retrieve")
                .node(retrieveNode(caller))
                .node(findCandidatesNode(caller))
                .node(notifyAndReassignNode(caller, agentRun.getId()))
                .edge("retrieve", state -> state.noLongerNeedsAttention() ? AgentGraph.END : "findCandidates")
                .edge("findCandidates", state -> "notifyAndReassign")
                .edge("notifyAndReassign", state -> AgentGraph.END)
                .build();

        PickupState finalState;
        try {
            finalState = graph.run(PickupState.initial(pickupTaskId));
        } catch (RuntimeException e) {
            log.warn("Pickup agent run {} failed for task {}", agentRun.getId(), pickupTaskId, e);
            agentRun.fail("Pickup delay check failed: " + e.getMessage());
            return new DelayCheckResult(agentRunRepository.save(agentRun));
        }

        finalizeOutcome(agentRun, finalState);
        return new DelayCheckResult(agentRunRepository.save(agentRun));
    }

    private void finalizeOutcome(AgentRun agentRun, PickupState state) {
        if (state.noLongerNeedsAttention()) {
            String status = state.task() != null ? state.task().status() : "unknown";
            agentRun.complete("Pickup task " + state.pickupTaskId() + " no longer needs attention (status=" + status + ").");
            return;
        }
        if (state.candidates() == null || state.candidates().isEmpty()) {
            agentRun.escalate("Pickup task " + state.pickupTaskId() + " is delayed (volunteer unresponsive) "
                    + "but no available replacement volunteers were found within " + searchRadiusKm
                    + "km — escalating to human ops.");
            return;
        }
        String summary = "Pickup task " + state.pickupTaskId() + " was delayed; notified " + state.notifiedCount()
                + " nearby available volunteer(s)" + (state.reassigned() ? "; task freed for reassignment." : ".");
        agentRun.complete(summary);
    }

    private GraphNode<PickupState> retrieveNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "retrieve";
            }

            @Override
            public PickupState execute(PickupState state) {
                var task = toolExecutor.run(getPickupTaskTool, caller, state.pickupTaskId());
                return state.withTask(task);
            }
        };
    }

    private GraphNode<PickupState> findCandidatesNode(AgentCallerContext caller) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "findCandidates";
            }

            @Override
            public PickupState execute(PickupState state) {
                var allCandidates = toolExecutor.run(findAvailableVolunteersTool, caller,
                        new FindAvailableVolunteersInput(state.pickupTaskId(), searchRadiusKm));
                var replacementCandidates = allCandidates.stream()
                        .filter(v -> !v.userId().equals(state.task().assignedVolunteerId()))
                        .toList();
                return state.withCandidates(replacementCandidates);
            }
        };
    }

    private GraphNode<PickupState> notifyAndReassignNode(AgentCallerContext caller, UUID agentRunId) {
        return new GraphNode<>() {
            @Override
            public String name() {
                return "notifyAndReassign";
            }

            @Override
            public PickupState execute(PickupState state) {
                if (state.candidates().isEmpty()) {
                    return state;
                }
                int notified = 0;
                for (VolunteerProfileDto candidate : state.candidates().stream().limit(notifyTopN).toList()) {
                    String etaNote = "";
                    if (candidate.latitude() != null && candidate.longitude() != null) {
                        RouteResult route = toolExecutor.run(calculateRouteTool, caller, new CalculateRouteInput(
                                candidate.latitude(), candidate.longitude(),
                                state.task().latitude(), state.task().longitude(), candidate.vehicleType()));
                        etaNote = " (about " + route.estimatedEtaMinutes() + " min away)";
                    }
                    toolExecutor.run(notifyVolunteerTool, caller, new NotifyVolunteerCommand(
                            candidate.userId(), "PUSH", "Pickup available nearby",
                            "A pickup task is running behind schedule and needs a volunteer" + etaNote + ".", agentRunId));
                    notified++;
                }

                toolExecutor.run(updatePickupStatusTool, caller, new UpdatePickupStatusCommand(state.pickupTaskId()));
                return state.withNotifiedCount(notified).withReassigned();
            }
        };
    }
}
