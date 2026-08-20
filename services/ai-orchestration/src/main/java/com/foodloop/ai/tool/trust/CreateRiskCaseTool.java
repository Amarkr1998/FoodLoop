package com.foodloop.ai.tool.trust;

import com.foodloop.ai.client.RiskCaseDto;
import com.foodloop.ai.client.TrustServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Trust &amp; Risk's only write tool (spec §21). Whatever the LLM wrote,
 * Trust's own {@code RiskCaseService#create} re-derives riskScore and
 * requiresHumanReview itself from its own Report data — this tool passes
 * only the human-readable rationale, never a score (docs/architecture/05
 * §4: "tool-side validation, not just prompt trust"). It cannot suspend or
 * ban a user; see RiskCase's Javadoc.
 */
@Component
public class CreateRiskCaseTool implements AgentTool<CreateRiskCaseCommand, RiskCaseDto> {

    private final TrustServiceClient trustServiceClient;

    public CreateRiskCaseTool(TrustServiceClient trustServiceClient) {
        this.trustServiceClient = trustServiceClient;
    }

    @Override
    public String name() {
        return "createRiskCase";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, CreateRiskCaseCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(CreateRiskCaseCommand input) {
        if (input.targetUserId() == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }
    }

    @Override
    public RiskCaseDto execute(CreateRiskCaseCommand input) {
        return trustServiceClient.createRiskCase(TenantContext.get(), input.targetUserId(), input.riskFactors());
    }

    @Override
    public void validateOutput(RiskCaseDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Trust service returned no risk case.");
        }
    }
}
