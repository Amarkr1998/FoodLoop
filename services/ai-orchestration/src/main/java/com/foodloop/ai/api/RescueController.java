package com.foodloop.ai.api;

import com.foodloop.ai.agent.rescue.RescueAgent;
import com.foodloop.commons.web.ApiException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual/ops trigger for a single listing+threshold check — the primary
 * trigger is {@link com.foodloop.ai.scheduler.RescueScheduler}'s automatic
 * sweep (spec §18); this exists for the same reason every other agent has
 * an HTTP entry point, and for testing/ops visibility into a specific
 * listing without waiting for the next scheduled tick.
 */
@RestController
public class RescueController {

    private final RescueAgent rescueAgent;

    public RescueController(RescueAgent rescueAgent) {
        this.rescueAgent = rescueAgent;
    }

    @PostMapping("/api/v1/ai/rescue/check")
    public RescueCheckResponse check(JwtAuthenticationToken authentication, @Valid @RequestBody RescueCheckRequest request) {
        RescueAgent.RescueResult result = rescueAgent.check(tenantId(authentication), request.foodListingId(), request.threshold());
        return RescueCheckResponse.from(result.agentRun());
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        String tenantClaim = authentication.getToken().getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return UUID.fromString(tenantClaim);
    }
}
