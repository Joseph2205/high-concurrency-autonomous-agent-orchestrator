package com.zaalima.high_concurrency_agent_orchestrator.dto;

public record AgentHeartbeatResponse(
        boolean alive,
        long serverTimeEpochMs) {
}
