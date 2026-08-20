package com.foodloop.ai.tool.trust;

import com.foodloop.ai.client.ReportDto;
import com.foodloop.ai.client.TrustServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Read tool granted to Trust & Risk (spec §21, §25) — the raw report list the LLM step summarizes into a rationale. */
@Component
public class GetReportHistoryTool implements AgentTool<UUID, List<ReportDto>> {

    private final TrustServiceClient trustServiceClient;

    public GetReportHistoryTool(TrustServiceClient trustServiceClient) {
        this.trustServiceClient = trustServiceClient;
    }

    @Override
    public String name() {
        return "getReportHistory";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("targetUserId must not be null");
        }
    }

    @Override
    public List<ReportDto> execute(UUID input) {
        return trustServiceClient.getReportHistory(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(List<ReportDto> output) {
        if (output == null) {
            throw new IllegalStateException("Trust service returned no report list.");
        }
    }
}
