package com.foodloop.ai.tool;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The permission matrix from docs/architecture/05-ai-agent-architecture.md §5
 * (spec §25), encoded rather than left as prose: no agent identity carries
 * {@code ROLE_ADMIN}, and each agent's allowed tool set is fixed here, not
 * derived from anything an LLM's own output claims about itself. A
 * prompt-injected agent can *ask* {@link ToolExecutor} for a tool outside its
 * row and the call is denied and audited, never silently allowed.
 *
 * <p>Business agents (Food Intelligence, Matching, Rescue, NGO Coordination,
 * Pickup, Trust &amp; Risk, Safety) are built in later phases, but the matrix
 * itself is an already-finalized architectural decision, so it is captured
 * here in full rather than as a single placeholder entry.
 */
@Component
public class AgentPermissionRegistry {

    private static final Map<String, Set<String>> PERMITTED_TOOLS = Map.ofEntries(
            Map.entry("food-intelligence", Set.of(
                    "getFoodListing", "classifyFoodImage", "updateFoodListingAiMetadata")),
            // docs/architecture/05 §5's table lists searchNearbyFood/searchNearbyNGOs/
            // getNGORequirements/checkFoodEligibility/calculateDistance for Matching,
            // written before Phase 7 scoping decided the NGO-request domain (Phase 8)
            // wasn't available yet — see FoodIntelligenceAgent's classifyFoodImage
            // precedent for the same "don't build a tool with nothing real behind it"
            // rule. checkFoodEligibility/calculateDistance fold into the deterministic
            // MatchingEngine inside createMatchProposal's server-side re-validation
            // instead of being separate agent-visible tool calls.
            Map.entry("matching", Set.of(
                    "getFoodListing", "searchNearbyReceivers", "createMatchProposal")),
            // createSafetyCase (Trust & Safety's escalation-marker tool, per §5's table)
            // isn't included: Trust & Safety doesn't exist yet either (Phase 9); Rescue's
            // own escalation is the standard AgentRun.escalate path every agent already
            // uses, not a separate tool call — see MatchingAgent's precedent.
            Map.entry("rescue", Set.of(
                    "getFoodListing", "searchNearbyReceivers", "sendNotification", "createMatchProposal")),
            // checkFoodEligibility/schedulePickup (also named in §5's table) aren't
            // separate tools: eligibility folds into the agent's own deterministic
            // candidate selection plus createMatchProposal's server-side
            // re-validation (same "matching" row precedent above); "schedulePickup"
            // is realized as the same createMatchProposal call NgoCoordinationAgent
            // already uses for the non-escalated path — see NgoCoordinationAgent's
            // Javadoc for why a distinct claim-on-behalf-of-the-NGO tool was
            // deliberately not built (it would require impersonating the NGO's own
            // user identity, a trust boundary this phase doesn't cross). getNGORequest
            // and getOrganization aren't in §5's table either but fill the same
            // "re-fetch the trigger's referenced entity before acting on it" role
            // getFoodListing plays for Food Intelligence/Matching/Rescue — see each
            // tool's own Javadoc.
            Map.entry("ngo-coordination", Set.of(
                    "getNGORequest", "getNGORequirements", "getOrganization", "searchNearbyFood", "createMatchProposal")),
            Map.entry("pickup", Set.of(
                    "findAvailableVolunteers", "calculateRoute", "sendNotification",
                    "updateFoodStatus")),
            Map.entry("trust-risk", Set.of(
                    "getUserBehaviorSignals", "getReportHistory", "createRiskCase")),
            Map.entry("safety", Set.of(
                    "getFoodListing", "createSafetyCase")));

    public boolean isPermitted(String agentName, String toolName) {
        return PERMITTED_TOOLS.getOrDefault(agentName, Set.of()).contains(toolName);
    }
}
