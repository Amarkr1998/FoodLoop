package com.foodloop.ai.tool.pickup;

import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import org.springframework.stereotype.Component;

/**
 * Pickup's calculateRoute tool (spec §20) — a pure, local computation via
 * {@link RouteCalculator}, no outbound call: unlike every other tool in this
 * package, there's no external state to fetch, just arithmetic over inputs
 * the agent already has (see AgentTool's contract — execute() isn't required
 * to make a network call, and this is still audited via ToolExecutor exactly
 * like one that does).
 */
@Component
public class CalculateRouteTool implements AgentTool<CalculateRouteInput, RouteResult> {

    @Override
    public String name() {
        return "calculateRoute";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, CalculateRouteInput input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(CalculateRouteInput input) {
        if (input == null || input.vehicleType() == null) {
            throw new IllegalArgumentException("vehicleType must not be null");
        }
    }

    @Override
    public RouteResult execute(CalculateRouteInput input) {
        double distanceMeters = RouteCalculator.distanceMeters(input.fromLat(), input.fromLng(), input.toLat(), input.toLng());
        int etaMinutes = RouteCalculator.estimatedEtaMinutes(distanceMeters, input.vehicleType());
        return new RouteResult(distanceMeters, etaMinutes);
    }

    @Override
    public void validateOutput(RouteResult output) {
        if (output == null) {
            throw new IllegalStateException("Route calculation produced no result.");
        }
    }
}
