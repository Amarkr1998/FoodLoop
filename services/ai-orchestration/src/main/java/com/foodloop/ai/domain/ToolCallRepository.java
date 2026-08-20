package com.foodloop.ai.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallRepository extends JpaRepository<ToolCallRecord, UUID> {

    List<ToolCallRecord> findByAgentRunId(UUID agentRunId);
}
