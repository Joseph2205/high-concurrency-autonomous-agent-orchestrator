package com.zaalima.high_concurrency_agent_orchestrator.dto;

import java.util.Map;

public record AgentSummaryResponse(
        String agentId,
        String agentName,
        String endpoint,
        Map<String, String> metadata,
        long registeredAtEpochMs,
        long lastHeartbeatEpochMs,
        long activeTasks,
        String nodeId) {
}
