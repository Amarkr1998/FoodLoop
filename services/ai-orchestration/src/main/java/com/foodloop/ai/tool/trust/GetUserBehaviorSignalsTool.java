package com.foodloop.ai.tool.trust;

import com.foodloop.ai.client.TrustServiceClient;
import com.foodloop.ai.client.UserBehaviorSignalDto;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Read tool granted to Trust & Risk (spec §21, §25) — a live-computed signal snapshot, never a stored aggregate. */
@Component
public class GetUserBehaviorSignalsTool implements AgentTool<UUID, UserBehaviorSignalDto> {

    private final TrustServiceClient trustServiceClient;

    public GetUserBehaviorSignalsTool(TrustServiceClient trustServiceClient) {
        this.trustServiceClient = trustServiceClient;
    }

    @Override
    public String name() {
        return "getUserBehaviorSignals";
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
    public UserBehaviorSignalDto execute(UUID input) {
        return trustServiceClient.getSignals(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(UserBehaviorSignalDto output) {
        if (output == null) {
            throw new IllegalStateException("Trust service returned no signal snapshot.");
        }
    }
}
