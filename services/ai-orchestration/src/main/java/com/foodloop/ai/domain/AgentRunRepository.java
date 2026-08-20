package com.foodloop.ai.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByAgentName(String agentName);

    /** The Rescue Agent's scheduled sweep (Phase 8) uses this to avoid re-notifying for a threshold it already handled. */
    List<AgentRun> findByAgentNameAndTriggerEventId(String agentName, UUID triggerEventId);
}
