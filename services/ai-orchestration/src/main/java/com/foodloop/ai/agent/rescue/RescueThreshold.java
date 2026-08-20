package com.foodloop.ai.agent.rescue;

/**
 * The two expiry checkpoints spec §18 describes (e.g. T-4h, T-1h). T-1h is
 * the "expand radius, then escalate" tier: reaching it always ends in
 * {@link com.foodloop.ai.domain.AgentRunStatus#ESCALATED}, whether or not
 * automated notification succeeded, since human ops needs visibility once
 * the deadline is this close regardless of outcome.
 */
public enum RescueThreshold {
    T_MINUS_4H,
    T_MINUS_1H
}
