package com.foodloop.ai.tool.matching;

import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Matching's only write tool. Whatever the LLM chose, Matching's own
 * {@code MatchingService#createProposal} re-fetches the listing and org and
 * re-derives eligibility/score itself — this tool is not the enforcement
 * point, just the call site (docs/architecture/05 §4).
 */
@Component
public class CreateMatchProposalTool implements AgentTool<CreateMatchProposalCommand, MatchProposalDto> {

    private final MatchingServiceClient matchingServiceClient;

    public CreateMatchProposalTool(MatchingServiceClient matchingServiceClient) {
        this.matchingServiceClient = matchingServiceClient;
    }

    @Override
    public String name() {
        return "createMatchProposal";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, CreateMatchProposalCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(CreateMatchProposalCommand input) {
        if (input.foodListingId() == null || input.receiverOrgId() == null) {
            throw new IllegalArgumentException("foodListingId and receiverOrgId are required");
        }
    }

    @Override
    public MatchProposalDto execute(CreateMatchProposalCommand input) {
        return matchingServiceClient.createProposal(
                TenantContext.get(), input.foodListingId(), input.receiverOrgId(), input.aiRationale());
    }

    @Override
    public void validateOutput(MatchProposalDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Matching service returned no proposal.");
        }
    }
}
